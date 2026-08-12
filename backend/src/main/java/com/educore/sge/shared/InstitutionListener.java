package com.educore.sge.shared;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class InstitutionListener {
    @PrePersist
    @PreUpdate
    @PreRemove
    public void setInstitution(Object entity) {
        if (entity instanceof BaseInstitutionEntity baseEntity) {
            if (baseEntity.getInstitutionId() == null) {
                String currentInstitution = InstitutionContext.getCurrentInstitution();
                if (currentInstitution != null) {
                    baseEntity.setInstitutionId(currentInstitution);
                } else {
                    throw new IllegalStateException("Seguridad: Intentando guardar datos sin una Institución activa.");
                }
            }
        }
    }
}
