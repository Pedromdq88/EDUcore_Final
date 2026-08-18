package com.educore.sge.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseInstitutionEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public String getInstitutionId() {
        return this.tenantId;
    }

    public void setInstitutionId(String institutionId) {
        this.tenantId = institutionId;
    }
}