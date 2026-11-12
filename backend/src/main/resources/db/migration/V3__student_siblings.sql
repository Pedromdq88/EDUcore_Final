CREATE TABLE IF NOT EXISTS student_siblings (
                                                tenant_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    sibling_id VARCHAR(36) NOT NULL,
    confirmed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (student_id, sibling_id),

    INDEX idx_student_siblings_tenant (tenant_id),
    INDEX idx_student_siblings_sibling (sibling_id),

    CONSTRAINT fk_siblings_tenant
    FOREIGN KEY (tenant_id)
    REFERENCES tenants(id) ON DELETE CASCADE,

    CONSTRAINT fk_siblings_student
    FOREIGN KEY (student_id)
    REFERENCES students(id) ON DELETE CASCADE,

    CONSTRAINT fk_siblings_other
    FOREIGN KEY (sibling_id)
    REFERENCES students(id) ON DELETE CASCADE,

    CONSTRAINT chk_student_siblings_order
    CHECK (student_id < sibling_id)
    );
