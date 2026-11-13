CREATE TABLE IF NOT EXISTS password_reset_tokens (
    user_id VARCHAR(36) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id)
        REFERENCES auth_users(id) ON DELETE CASCADE
);
