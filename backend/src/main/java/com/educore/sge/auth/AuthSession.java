package com.educore.sge.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id @Column(name = "token_hash", length = 64, nullable = false) public String tokenHash;
    @Column(name = "user_id", length = 36, nullable = false) public String userId;
    // Compatibilidad con esquemas anteriores que tienen account_id NOT NULL.
    @Column(name = "account_id", length = 36) public String accountId;
    @Column(name = "tenant_id", length = 36, nullable = false) public String tenantId;
    @Column(name = "expires_at", nullable = false) public Instant expiresAt;
}
