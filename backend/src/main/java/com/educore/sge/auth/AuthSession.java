package com.educore.sge.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id @Column(name = "token_hash", length = 64)
    public String tokenHash;
    @Column(name = "account_id", nullable = false, length = 36)
    public String accountId;
    @Column(name = "tenant_id", nullable = false, length = 36)
    public String tenantId;
    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;
}
