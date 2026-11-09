package com.educore.sge.auth;

import com.educore.sge.institution.infrastructure.entity.TenantJpaEntity;
import com.educore.sge.institution.infrastructure.repository.TenantJpaRepository;
import com.educore.sge.shared.Rol;
import com.educore.sge.shared.domain.EducationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

/** Provisionamiento explícito, sin contraseñas de demostración grabadas en el código. */
@Component
public class DirectorBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DirectorBootstrap.class);
    private final AuthUserRepository users;
    private final TenantJpaRepository tenants;
    private final PasswordEncoder encoder;

    @Value("${educore.bootstrap.director-email:}") private String email;
    @Value("${educore.bootstrap.director-password:}") private String password;
    @Value("${educore.bootstrap.tenant-id:}") private String tenantId;
    @Value("${educore.bootstrap.school-name:}") private String schoolName;

    public DirectorBootstrap(AuthUserRepository users, TenantJpaRepository tenants, PasswordEncoder encoder) {
        this.users = users; this.tenants = tenants; this.encoder = encoder;
    }
    @Override public void run(ApplicationArguments ignored) {
        if (email.isBlank() && password.isBlank()) {
            log.warn("Director aún no provisionado. Establecé EDUCORE_DIRECTOR_EMAIL y EDUCORE_DIRECTOR_PASSWORD en el entorno.");
            return;
        }
        if (email.isBlank() || password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException("DIRECTOR: configurá correo y contraseña entre 12 y 72 caracteres; no se permiten credenciales vacías.");
        }
        List<TenantJpaEntity> schools;
        if (!tenantId.isBlank()) {
            schools = List.of(tenants.findById(tenantId).orElseThrow(() -> new IllegalStateException("Institución configurada para el director no existe: " + tenantId)));
        } else {
            schools = tenants.findAll();
            if (schools.isEmpty() && !schoolName.isBlank()) {
                TenantJpaEntity school = new TenantJpaEntity();
                school.setId(UUID.randomUUID().toString());
                school.setName(schoolName);
                school.setEducationLevel(EducationLevel.JARDIN);
                schools = List.of(tenants.save(school));
                log.info("Institución inicial creada: {}", school.getName());
            }
            if (schools.size() != 1) {
                throw new IllegalStateException("Para provisionar un director se requiere exactamente una institución o EDUCORE_DIRECTOR_TENANT_ID.");
            }
        }
        TenantJpaEntity school = schools.get(0);
        if (users.findByTenantIdAndEmailIgnoreCase(school.getId(), email.trim()).isPresent()) {
            log.info("La cuenta de director ya está registrada para {} (sin modificar su contraseña).", school.getName());
            return;
        }
        AuthUser user = new AuthUser();
        user.id = UUID.randomUUID().toString();
        user.email = email.trim().toLowerCase(java.util.Locale.ROOT);
        user.passwordHash = encoder.encode(password);
        user.tenantId = school.getId();
        user.role = Rol.DIRECTOR;
        users.save(user);
        log.info("Cuenta DIRECTOR creada para {} (correo: {}).", school.getName(), user.email);
    }
}
