package com.educore.sge.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import com.educore.sge.shared.Rol;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.institution.infrastructure.entity.StaffJpaEntity;
import com.educore.sge.institution.infrastructure.repository.StaffJpaRepository;

@Service
public class AuthService {
    private final AuthUserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthUserRepository users, AuthSessionRepository sessions, PasswordEncoder passwords) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
    }

    public static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) { throw new IllegalStateException("SHA-256 no disponible", ex); }
    }

    @Transactional
    public LoginResult login(String tenantId, String email, String password) {
        if (tenantId == null || tenantId.isBlank() || email == null || email.isBlank() || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Institución, correo y contraseña son obligatorios.");
        }
        AuthUser user = users.findByTenantIdAndEmailIgnoreCase(tenantId, email.trim()).orElse(null);
        if (user == null || !user.enabled || !passwords.matches(password, user.passwordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas.");
        }
        byte[] random = new byte[32]; secureRandom.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        AuthSession session = new AuthSession();
        session.tokenHash = hashToken(token);
        session.userId = user.id;
        session.accountId = user.id;
        session.tenantId = user.tenantId;
        session.expiresAt = Instant.now().plus(8, ChronoUnit.HOURS);
        sessions.save(session);
        return new LoginResult(token, user.email, user.role.name(), user.tenantId, session.expiresAt.toString(), user.mustChangePassword);
    }

    @Transactional(readOnly = true)
    public Optional<AuthUser> authenticate(String token, String tenantId) {
        if (token == null || token.isBlank() || tenantId == null || tenantId.isBlank()) return Optional.empty();
        return sessions.findById(hashToken(token))
            .filter(s -> s.expiresAt.isAfter(Instant.now()) && tenantId.equals(s.tenantId))
            .flatMap(s -> users.findById(s.userId))
            .filter(u -> u.enabled && tenantId.equals(u.tenantId));
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) sessions.deleteById(hashToken(token));
    }

    @Transactional
    public void changePassword(String tenantId, String email, String currentPassword, String newPassword, String token) {
        AuthUser user = users.findByTenantIdAndEmailIgnoreCase(tenantId, email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!passwords.matches(currentPassword, user.passwordHash)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contraseña actual incorrecta");
        if (newPassword == null || newPassword.length() < 12 || newPassword.length() > 72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener entre 12 y 72 caracteres");
        user.passwordHash = passwords.encode(newPassword);
        user.mustChangePassword = false;
        users.save(user);
        sessions.deleteAll(sessions.findAll().stream().filter(s -> s.userId.equals(user.id)).toList());
    }

    /** Creación transaccional desde el alta del legajo; nunca guarda DNI como contraseña en texto plano. */
    @Transactional
    public boolean provisionTutorAccount(TutorJpaEntity tutor, String tenantId) {
        String email = tutor.getEmail() == null ? "" : tutor.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        String dni = tutor.getDocumentNumber() == null ? "" : tutor.getDocumentNumber().replaceAll("[^0-9]", "");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Para dar de alta la cuenta de tutor, informá un correo válido.");
        }
        if (dni.length() < 6 || dni.length() > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El DNI debe contener entre 6 y 12 dígitos para generar la clave temporal.");
        }
        if (users.findByTenantIdAndTutorId(tenantId, tutor.getId()).isPresent()) return false;
        AuthUser existingByEmail = users.findByTenantIdAndEmailIgnoreCase(tenantId, email).orElse(null);
        if (existingByEmail != null) {
            // Recuperación explícita por Dirección/Administración de cuentas antiguas:
            // solo se permite cuando todavía no están vinculadas a ninguna ficha,
            // tienen rol TUTOR, están habilitadas y el correo pertenece a la misma institución.
            // Esto NO asigna alumnos. Los vínculos siguen definidos en student_tutors.
            if (existingByEmail.enabled && existingByEmail.role == Rol.TUTOR &&
                (existingByEmail.tutorId == null || existingByEmail.tutorId.isBlank()) &&
                (existingByEmail.staffId == null || existingByEmail.staffId.isBlank())) {
                existingByEmail.tutorId = tutor.getId();
                users.save(existingByEmail);
                revokeSessions(existingByEmail.id);
                return true;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ese correo ya tiene una cuenta vinculada o deshabilitada. Revisá el legajo antes de activarla.");
        }
        AuthUser account = new AuthUser();
        account.id = UUID.randomUUID().toString();
        account.tenantId = tenantId;
        account.email = email;
        account.passwordHash = passwords.encode(dni);
        account.role = Rol.TUTOR;
        account.tutorId = tutor.getId();
        account.mustChangePassword = true;
        account.enabled = true;
        users.save(account);
        return true;
    }

    /** La edición del correo del tutor no debe crear una cuenta nueva ni perder sus permisos. */
    @Transactional
    public void syncTutorEmail(TutorJpaEntity tutor) {
        users.findByTenantIdAndTutorId(tutor.getTenantId(), tutor.getId()).ifPresent(user -> {
            String email = tutor.getEmail() == null ? "" : tutor.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo de tutor inválido.");
            }
            users.findByTenantIdAndEmailIgnoreCase(tutor.getTenantId(), email).ifPresent(other -> {
                if (!other.id.equals(user.id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este correo ya pertenece a otra cuenta.");
            });
            user.email = email;
            users.save(user);
        });
    }

    @Transactional
    public void disableTutorAccount(String tenantId, String tutorId) {
        users.findByTenantIdAndTutorId(tenantId, tutorId).ifPresent(user -> {
            user.enabled = false;
            users.save(user);
            sessions.deleteAll(sessions.findAll().stream().filter(s -> s.userId.equals(user.id)).toList());
        });
    }

    /** Alta del personal con cuenta real: DNI normalizado, BCrypt y cambio obligatorio. */
    @Transactional
    public boolean provisionStaffAccount(StaffJpaEntity staff) {
        String tenant = staff.getTenantId();
        String email = staff.getEmail() == null ? "" : staff.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        String dni = staff.getDocumentNumber() == null ? "" : staff.getDocumentNumber().replaceAll("[^0-9]", "");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El personal necesita un correo válido para ingresar.");
        if (staff.getRole() == null || staff.getRole() == Rol.TUTOR || staff.getRole() == Rol.OWNER)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rol de personal no válido.");
        AuthUser linked = users.findByTenantIdAndStaffId(tenant, staff.getId()).orElse(null);
        AuthUser byEmail = users.findByTenantIdAndEmailIgnoreCase(tenant, email).orElse(null);
        if (linked != null) {
            if (byEmail != null && !linked.id.equals(byEmail.id))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"El correo ya pertenece a otra cuenta.");
            linked.email = email;
            linked.role = staff.getRole();
            linked.enabled = "ACTIVE".equalsIgnoreCase(staff.getStatus());
            users.save(linked);
            revokeSessions(linked.id); // Role/email change takes effect immediately.
            return false;
        }
        if (byEmail != null) {
            // Vincula la cuenta del Director cargada con el SQL original sin resetear su clave.
            if (byEmail.staffId == null && byEmail.tutorId == null && byEmail.role == staff.getRole()) {
                byEmail.staffId = staff.getId();
                byEmail.enabled = true;
                users.save(byEmail);
                return false;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Ese correo ya tiene un usuario en la escuela.");
        }
        if (dni.length() < 6 || dni.length() > 12)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ingresá un DNI de 6 a 12 dígitos para crear la cuenta del personal.");
        AuthUser account = new AuthUser();
        account.id = UUID.randomUUID().toString();
        account.tenantId = tenant;
        account.staffId = staff.getId();
        account.email = email;
        account.passwordHash = passwords.encode(dni);
        account.role = staff.getRole();
        account.enabled = true;
        account.mustChangePassword = true;
        users.save(account);
        return true;
    }

    @Transactional
    public void disableStaffAccount(String tenantId, String staffId) {
        users.findByTenantIdAndStaffId(tenantId, staffId).ifPresent(user -> {
            user.enabled = false;
            users.save(user);
            revokeSessions(user.id);
        });
    }

    private void revokeSessions(String userId) {
        sessions.deleteAll(sessions.findAll().stream().filter(s -> s.userId.equals(userId)).toList());
    }

    public record LoginResult(String token, String email, String role, String institutionId, String expiresAt, boolean requiresPasswordChange) {}
}
