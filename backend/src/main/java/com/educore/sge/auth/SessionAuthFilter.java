package com.educore.sge.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/** Solo un token emitido por /auth/login puede establecer la identidad y el rol. */
public class SessionAuthFilter extends OncePerRequestFilter {
    private final AuthService auth;
    public SessionAuthFilter(AuthService auth) { this.auth = auth; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            String tenantId = request.getHeader("X-Institution-Id");
            var account = auth.authenticate(token, tenantId);
            if (account.isPresent() && account.get().mustChangePassword) {
                String path = request.getRequestURI();
                if (!path.equals("/api/v1/auth/change-password") &&
                    !path.equals("/api/v1/auth/logout") && !path.equals("/api/v1/auth/me")) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Tenés que cambiar la contraseña inicial antes de usar el sistema.");
                    return;
                }
            }
            account.ifPresent(u -> {
                var authentication = new UsernamePasswordAuthenticationToken(u.email, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + u.role.name())));
                authentication.setDetails(u.tenantId);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        try { chain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
