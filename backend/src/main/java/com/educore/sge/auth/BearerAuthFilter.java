package com.educore.sge.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Verifica el JWT y consulta la base de datos para permitir revocacion/logout. */
public class BearerAuthFilter extends OncePerRequestFilter {
    private final AuthAccountRepository accounts;
    private final AuthSessionRepository sessions;
    private final JwtTokenService jwtTokens;

    public BearerAuthFilter(AuthAccountRepository accounts, AuthSessionRepository sessions,
                            JwtTokenService jwtTokens) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.jwtTokens = jwtTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String authorization = req.getHeader("Authorization");
        if (authorization != null) {
            if (!authorization.startsWith("Bearer ") || authorization.length() <= 7) {
                res.sendError(401, "Token de acceso invalido"); return;
            }
            String token = authorization.substring(7).trim();
            DecodedJWT jwt;
            try {
                jwt = jwtTokens.verifyAccessToken(token);
            } catch (JWTVerificationException | IllegalArgumentException ex) {
                res.sendError(401, "JWT invalido o vencido"); return;
            }
            var session = sessions.findById(AuthTokens.hash(token));
            if (session.isEmpty() || !session.get().expiresAt.isAfter(Instant.now())) {
                res.sendError(401, "Sesion cerrada o vencida"); return;
            }
            var found = accounts.findById(jwt.getSubject());
            if (found.isEmpty() || !found.get().enabled) {
                res.sendError(401, "Cuenta inactiva"); return;
            }
            AuthAccount a = found.get();
            // No confiar en el rol ni la institucion del navegador: contrastar JWT y base de datos.
            if (!a.id.equals(session.get().accountId)
                || !a.tenantId.equals(session.get().tenantId)
                || !a.tenantId.equals(jwt.getClaim("tenant_id").asString())
                || !a.role.equals(jwt.getClaim("role").asString())) {
                res.sendError(401, "Credenciales desactualizadas"); return;
            }
            String institutionHeader = req.getHeader("X-Institution-Id");
            if (institutionHeader != null && !institutionHeader.equals(a.tenantId)) {
                res.sendError(403, "No podés acceder a otra institucion"); return;
            }
            var principal = new AuthPrincipal(a.id, a.tenantId, a.email, a.role, a.personType, a.personId);
            var authentication = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + a.role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        try { chain.doFilter(req, res); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
