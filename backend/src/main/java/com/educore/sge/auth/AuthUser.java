package com.educore.sge.auth;

import com.educore.sge.shared.Rol;
import jakarta.persistence.*;

@Entity
@Table(name = "auth_users", uniqueConstraints = @UniqueConstraint(name = "uk_auth_users_tenant_email", columnNames = {"tenant_id", "email"}))
public class AuthUser {
    @Id @Column(length = 36, nullable = false) public String id;
    @Column(name = "tenant_id", length = 36, nullable = false) public String tenantId;
    @Column(nullable = false, length = 190) public String email;
    @Column(name = "password_hash", nullable = false, length = 100) public String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public Rol role;
    @Column(nullable = false) public boolean enabled = true;
    // Identidad real de tutor asociada al legajo (nunca derivar acceso solo del email).
    @Column(name = "tutor_id", length = 36) public String tutorId;
    @Column(name = "staff_id", length = 36) public String staffId;
    @Column(name = "must_change_password", nullable = false) public boolean mustChangePassword = false;
}
