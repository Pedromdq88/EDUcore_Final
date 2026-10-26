package com.educore.sge.auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthAccountRepository accounts;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final GmailSender mail;
    private final JwtTokenService jwtTokens;
    @Value("${educore.frontend-url:http://127.0.0.1:5500/index.html}") private String frontendUrl;

    public AuthService(AuthAccountRepository accounts, AuthSessionRepository sessions, PasswordEncoder passwords,
                       GmailSender mail, JwtTokenService jwtTokens) {
        this.accounts = accounts; this.sessions = sessions; this.passwords = passwords;
        this.mail = mail; this.jwtTokens = jwtTokens;
    }
    public record LoginResult(String token, String email, String role, String institutionId) {}

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResult login(String institutionId, String email, String password) {
        if (institutionId == null || email == null || password == null || password.isBlank())
            throw invalidLogin();
        AuthAccount account = accounts.findByTenantIdAndEmailIgnoreCase(institutionId, normalize(email))
                .orElseThrow(this::invalidLogin);
        if (account.lockedUntil != null && account.lockedUntil.isAfter(Instant.now())) throw invalidLogin();
        if (!account.enabled || !passwords.matches(password, account.passwordHash)) {
            account.failedAttempts++;
            if (account.failedAttempts >= 5) { account.lockedUntil = Instant.now().plus(Duration.ofMinutes(15)); account.failedAttempts = 0; }
            accounts.save(account);
            throw invalidLogin();
        }
        account.failedAttempts = 0; account.lockedUntil = null;
        accounts.save(account);
        // El token de acceso es JWT, no un token aleatorio opaco.
        String token = jwtTokens.createAccessToken(account);
        AuthSession session = new AuthSession();
        session.tokenHash = AuthTokens.hash(token);
        session.accountId = account.id;
        session.tenantId = account.tenantId;
        session.expiresAt = jwtTokens.expiration(token);
        sessions.save(session);
        return new LoginResult(token, account.email, account.role, account.tenantId);
    }
    private ResponseStatusException invalidLogin() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }
    public AuthPrincipal principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sin sesión");
        return p;
    }
    @Transactional
    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer "))
            sessions.deleteById(AuthTokens.hash(authorization.substring(7).trim()));
    }
    @Transactional
    public void changePassword(String previous, String next) {
        AuthAccount account = accounts.findById(principal().id()).orElseThrow(this::invalidLogin);
        if (previous == null || !passwords.matches(previous, account.passwordHash)) throw invalidLogin();
        passwordPolicy(next);
        account.passwordHash = passwords.encode(next);
        accounts.save(account);
        revokeAllSessions(account.id);
    }
    @Transactional
    public void forgotPassword(String institutionId, String email) {
        if (institutionId == null || email == null) return;
        accounts.findByTenantIdAndEmailIgnoreCase(institutionId, normalize(email)).ifPresent(a -> {
            Instant now = Instant.now();
            if (a.lastResetSentAt != null && a.lastResetSentAt.plus(Duration.ofMinutes(2)).isAfter(now)) return;
            invite(a, false);
        });
    }
    @Transactional
    public void resetPassword(String token, String newPassword) {
        passwordPolicy(newPassword);
        if (token == null || token.length() != 43) throw expiredToken();
        AuthAccount account = accounts.findByResetTokenHash(AuthTokens.hash(token)).orElseThrow(this::expiredToken);
        if (account.resetExpiresAt == null || !account.resetExpiresAt.isAfter(Instant.now())) throw expiredToken();
        account.passwordHash = passwords.encode(newPassword);
        account.enabled = true;
        account.failedAttempts = 0;
        account.lockedUntil = null;
        account.resetTokenHash = null;
        account.resetExpiresAt = null;
        accounts.save(account);
        revokeAllSessions(account.id);
    }
    private ResponseStatusException expiredToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enlace inválido o vencido");
    }
    private static void passwordPolicy(String value) {
        if (value == null || value.length() < 12 || value.length() > 128)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener entre 12 y 128 caracteres");
    }
    private void revokeAllSessions(String accountId) {
        sessions.deleteAll(sessions.findByAccountId(accountId));
    }
    private static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private static void checkEmail(String email) {
        if (email == null || email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo electrónico es obligatorio y debe ser válido");
    }
    private void invite(AuthAccount account, boolean firstTime) {
        String token = AuthTokens.generate();
        account.resetTokenHash = AuthTokens.hash(token);
        account.resetExpiresAt = Instant.now().plus(Duration.ofMinutes(30));
        account.lastResetSentAt = Instant.now();
        accounts.saveAndFlush(account);
        String url = frontendUrl + (frontendUrl.contains("?") ? "&" : "?") + "reset=" + token;
        mail.send(account.email,
                firstTime ? "Activá tu cuenta de EduCore" : "Recuperación de contraseña de EduCore",
                "Hola.\n\n" + (firstTime ? "Tu cuenta de EduCore fue creada." : "Recibimos una solicitud para cambiar tu contraseña.")
                        + "\nIngresá a este enlace para establecer tu contraseña (vence en 30 minutos):\n" + url
                        + "\n\nSi no solicitaste esto, ignorá el mensaje. No compartas este enlace.");
    }
    @Transactional
    public void provision(String tenantId, String personType, String personId, String email, String role) {
        checkEmail(email);
        if (!(role.equals("DIRECTOR") || role.equals("ADMINISTRATIVE")
                || role.equals("TEACHER") || role.equals("TUTOR")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no permitido");
        if (!(personType.equals("STAFF") || personType.equals("TUTOR"))
                || (role.equals("TUTOR") != personType.equals("TUTOR")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol y tipo de persona no coinciden");
        AuthPrincipal actor = principal();
        if (!actor.tenantId().equals(tenantId)
                || !(actor.role().equals("DIRECTOR") || actor.role().equals("ADMINISTRATIVE")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede asignar usuarios");
        String normal = normalize(email);
        AuthAccount a = accounts.findByTenantIdAndPersonTypeAndPersonId(tenantId, personType, personId)
                .orElseGet(AuthAccount::new);
        // Solo el director puede dar de alta administradores.
        // Un administrativo solo puede crear o editar docentes y tutores.
        if (actor.role().equals("ADMINISTRATIVE")
                && (!(role.equals("TEACHER") || role.equals("TUTOR"))
                || (a.role != null && !(a.role.equals("TEACHER") || a.role.equals("TUTOR")))))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrativo: solo puede administrar docentes y tutores");
        // El primer director se crea mediante el alta inicial, no por este formulario.
        if (role.equals("DIRECTOR") && a.id == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Las cuentas de director no se crean desde este formulario");
        var claimed = accounts.findByTenantIdAndEmailIgnoreCase(tenantId, normal);
        if (claimed.isPresent() && !claimed.get().id.equals(a.id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo en la institución");
        boolean emailChanged = a.email != null && !a.email.equalsIgnoreCase(normal);
        boolean roleChanged = a.role != null && !a.role.equals(role);
        boolean first = a.id == null;
        if (first) { a.id = UUID.randomUUID().toString(); a.passwordHash = passwords.encode(AuthTokens.generate()); a.enabled = false; }
        a.tenantId = tenantId; a.personType = personType; a.personId = personId;
        a.email = normal; a.role = role;
        if (emailChanged) { a.enabled = false; revokeAllSessions(a.id); }
        if (roleChanged) revokeAllSessions(a.id);
        accounts.saveAndFlush(a);
        if (first || emailChanged || (!a.enabled &&
                (a.lastResetSentAt == null || a.lastResetSentAt.plus(Duration.ofMinutes(2)).isBefore(Instant.now()))))
            invite(a, true);
    }
    @Transactional
    public void revoke(String tenantId, String personType, String personId) {
        accounts.findByTenantIdAndPersonTypeAndPersonId(tenantId, personType, personId).ifPresent(a -> {
            revokeAllSessions(a.id);
            accounts.delete(a);
        });
    }
}


