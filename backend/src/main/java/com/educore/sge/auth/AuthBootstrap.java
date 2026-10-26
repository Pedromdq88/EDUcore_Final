package com.educore.sge.auth;

import com.educore.sge.institution.infrastructure.repository.TenantJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.UUID;

/** Primer director: se crea una sola vez, únicamente con 3 variables de entorno explícitas. */
@Component
public class AuthBootstrap implements CommandLineRunner {
    private final Environment env;
    private final AuthAccountRepository accounts;
    private final TenantJpaRepository tenants;
    private final PasswordEncoder passwords;
    public AuthBootstrap(Environment env, AuthAccountRepository accounts, TenantJpaRepository tenants, PasswordEncoder passwords) {
        this.env=env; this.accounts=accounts; this.tenants=tenants; this.passwords=passwords;
    }
    @Override @Transactional
    public void run(String... args) {
        String tenant = env.getProperty("EDUCORE_BOOTSTRAP_TENANT_ID", "");
        String email = env.getProperty("EDUCORE_BOOTSTRAP_EMAIL", "");
        String password = env.getProperty("EDUCORE_BOOTSTRAP_PASSWORD", "");
        if (tenant.isBlank() && email.isBlank() && password.isBlank()) return;
        if (tenant.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || password.length()<12)
            throw new IllegalStateException("Variables de bootstrap incompletas o contraseña menor a 12 caracteres");
        if (!tenants.existsById(tenant)) throw new IllegalStateException("EDUCORE_BOOTSTRAP_TENANT_ID no existe");
        if (accounts.existsByTenantId(tenant)) return; // Nunca crea una segunda cuenta privilegiada.
        AuthAccount a = new AuthAccount();
        a.id=UUID.randomUUID().toString(); a.tenantId=tenant; a.email=email.strip().toLowerCase(Locale.ROOT);
        a.personType="BOOTSTRAP"; a.personId=UUID.randomUUID().toString();
        a.role="DIRECTOR"; a.passwordHash=passwords.encode(password); a.enabled=true;
        accounts.save(a);
    }
}
