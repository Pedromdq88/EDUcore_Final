package com.educore.sge.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_accounts")
public class AuthAccount {
    @Id @Column(length = 36)
    public String id;
    @Column(name = "tenant_id", nullable = false, length = 36)
    public String tenantId;
    @Column(nullable = false, length = 150)
    public String email;
    @Column(nullable = false, length = 50)
    public String role;
    @Column(name = "person_type", nullable = false, length = 25)
    public String personType;
    @Column(name = "person_id", nullable = false, length = 36)
    public String personId;
    @Column(name = "password_hash", nullable = false, length = 255)
    public String passwordHash;
    @Column(nullable = false)
    public boolean enabled;
    @Column(name = "failed_attempts", nullable = false)
    public int failedAttempts;
    @Column(name = "locked_until")
    public Instant lockedUntil;
    @Column(name = "reset_token_hash", length = 64)
    public String resetTokenHash;
    @Column(name = "reset_expires_at")
    public Instant resetExpiresAt;
    @Column(name = "last_reset_sent_at")
    public Instant lastResetSentAt;
}
