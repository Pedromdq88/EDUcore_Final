package com.educore.sge.shared;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@EntityListeners(InstitutionListener.class)
@FilterDef(name = "institutionFilter", parameters = {@ParamDef(name = "institutionId", type = String.class)})
@Filter(name = "institutionFilter", condition = "tenant_id = :institutionId")
public abstract class BaseInstitutionEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String institutionId;

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }
}
