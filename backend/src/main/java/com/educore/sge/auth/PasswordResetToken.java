package com.educore.sge.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One active reset code per account; never stores a code in plaintext. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @Column(name = "user_id", length = 36, nullable = false)
    public String userId;

    @Column(name = "code_hash", length = 100, nullable = false)
    public String codeHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "attempt_count", nullable = false)
    public int attemptCount;
}
