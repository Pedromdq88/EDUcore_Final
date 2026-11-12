package com.educore.sge.auth;

import com.educore.sge.auth.mail.GmailApiSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private static final String GENERIC_MESSAGE =
            "Si la cuenta está habilitada, te enviamos un código al correo registrado.";

    private final PasswordResetTokenRepository codes;
    private final AuthUserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final GmailApiSender gmail;
    private final SecureRandom random = new SecureRandom();
    // Local deployment rate limits; production with multiple instances should use Redis/shared storage.
    private final Map<String, Long> lastRequest = new ConcurrentHashMap<>();
    private final Map<String, Window> ipWindows = new ConcurrentHashMap<>();

    public PasswordRecoveryService(PasswordResetTokenRepository codes, AuthUserRepository users,
            AuthSessionRepository sessions, PasswordEncoder encoder, GmailApiSender gmail) {
        this.codes = codes;
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.gmail = gmail;
    }

    public record Request(String institutionId, String email) {}
    public record Confirm(String institutionId, String email, String code, String newPassword) {}
    public record Verify(String institutionId, String email, String code) {}
    public record VerifyResult(boolean valid, String message) {}
    public record Message(String message) {}
    private static final class Window {
        long beginning;
        int count;
        Window(long beginning) { this.beginning = beginning; }
    }

    @Transactional
    public Message request(Request input, String ipAddress) {
        if (input == null || input.institutionId() == null || input.institutionId().isBlank() ||
                input.email() == null || input.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí una institución e ingresá el correo.");
        }
        if (!gmail.isReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "La recuperación por Gmail aún no está configurada en esta instalación.");
        String email = input.email().trim().toLowerCase(Locale.ROOT);
        if (email.length() > 190) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo inválido");
        String tenantId = input.institutionId().trim();
        long now = System.currentTimeMillis();
        if (!ipAllowance(ipAddress, now)) return new Message(GENERIC_MESSAGE);
        String key = AuthService.hashToken(tenantId + "|" + email);
        // 90 seconds between messages to the same account, even across browsers/IPs.
        Long previous = lastRequest.putIfAbsent(key, now);
        if (previous != null) {
            if (now - previous < 90_000) return new Message(GENERIC_MESSAGE);
            if (!lastRequest.replace(key, previous, now)) return new Message(GENERIC_MESSAGE);
        }
        var account = users.findByTenantIdAndEmailIgnoreCase(tenantId, email).orElse(null);
        if (account == null || !account.enabled) return new Message(GENERIC_MESSAGE);
        // The seeded local Director is not a verified mailbox. Never email admin reset codes
        // to this illustrative address, which might belong to someone outside the school.
        if ("b18ca367-da22-11f1-a30a-d85ed3de6a1e".equals(account.id) &&
                "director@educore.com".equalsIgnoreCase(account.email) &&
                account.role == com.educore.sge.shared.Rol.DIRECTOR) {
            return new Message(GENERIC_MESSAGE);
        }
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        PasswordResetToken token = codes.findById(account.id).orElseGet(PasswordResetToken::new);
        token.userId = account.id;
        token.codeHash = encoder.encode(code);
        token.attemptCount = 0;
        token.createdAt = Instant.now();
        token.expiresAt = token.createdAt.plus(TTL);
        codes.saveAndFlush(token);
        try {
            gmail.sendResetCode(account.email, code);
        } catch (RuntimeException ex) {
            // Generic server failure avoids revealing if the requested account exists.
            log.error("Fallo Gmail en recuperación; revisar OAuth2 y remitente", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de correo no está disponible temporalmente.");
        }
        return new Message(GENERIC_MESSAGE);
    }

    /** First-stage code validation. Does not consume the code or authorize a reset by itself.
     * The reset-password endpoint independently rechecks the code before changing anything. */
    @Transactional
    public VerifyResult verify(Verify input) {
        String rejected = "Código incorrecto o vencido. Revisalo e intentá nuevamente.";
        if (input == null || input.institutionId() == null || input.institutionId().isBlank() ||
                input.email() == null || input.email().isBlank() ||
                input.code() == null || !input.code().matches("[0-9]{6}")) {
            return new VerifyResult(false, rejected);
        }
        String tenant = input.institutionId().trim();
        String email = input.email().trim().toLowerCase(Locale.ROOT);
        var account = users.findByTenantIdAndEmailIgnoreCase(tenant, email)
                .filter(u -> u.enabled).orElse(null);
        if (account == null) return new VerifyResult(false, rejected);

        PasswordResetToken token = codes.lockByUserId(account.id).orElse(null);
        if (token == null) return new VerifyResult(false, rejected);
        if (!token.expiresAt.isAfter(Instant.now()) || token.attemptCount >= MAX_ATTEMPTS) {
            codes.delete(token);
            return new VerifyResult(false, rejected);
        }
        if (!encoder.matches(input.code(), token.codeHash)) {
            token.attemptCount++;
            codes.save(token);
            return new VerifyResult(false, rejected);
        }
        return new VerifyResult(true, "Código verificado. Elegí tu nueva contraseña.");
    }

    @Transactional
    public Message confirm(Confirm input) {
        if (input == null || input.institutionId() == null || input.email() == null ||
                input.code() == null || !input.code().matches("[0-9]{6}")) {
            throw invalidCode();
        }
        if (input.newPassword() == null || input.newPassword().length() < 12 ||
                input.newPassword().length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener entre 12 y 72 caracteres.");
        }
        String tenant = input.institutionId().trim();
        String email = input.email().trim();
        AuthUser user = users.findByTenantIdAndEmailIgnoreCase(tenant, email)
                .filter(u -> u.enabled).orElseThrow(PasswordRecoveryService::invalidCode);
        PasswordResetToken reset = codes.lockByUserId(user.id)
                .orElseThrow(PasswordRecoveryService::invalidCode);
        if (reset.expiresAt.isBefore(Instant.now()) || reset.attemptCount >= MAX_ATTEMPTS) {
            codes.delete(reset);
            throw invalidCode();
        }
        reset.attemptCount++;
        if (!encoder.matches(input.code(), reset.codeHash)) {
            codes.save(reset);
            // Do not rollback unsuccessful-attempt accounting.
            return new Message("El código es incorrecto o expiró. Revisalo e intentá nuevamente.");
        }
        if (encoder.matches(input.newPassword(), user.passwordHash)) {
            return new Message("Elegí una contraseña diferente a la actual.");
        }
        user.passwordHash = encoder.encode(input.newPassword());
        user.mustChangePassword = false;
        users.save(user);
        codes.delete(reset);
        sessions.deleteAll(sessions.findAll().stream().filter(s -> user.id.equals(s.userId)).toList());
        return new Message("Contraseña actualizada. Ahora podés iniciar sesión.");
    }

    private boolean ipAllowance(String ip, long now) {
        String safeIp = ip == null ? "unknown" : ip;
        if (ipWindows.size() > 10_000) ipWindows.clear();
        Window window = ipWindows.computeIfAbsent(safeIp, k -> new Window(now));
        synchronized (window) {
            if (now - window.beginning > 3_600_000) { window.beginning = now; window.count = 0; }
            return ++window.count <= 20;
        }
    }

    private static ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o vencido.");
    }
}
