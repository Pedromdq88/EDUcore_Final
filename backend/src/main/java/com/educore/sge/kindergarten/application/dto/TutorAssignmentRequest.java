package com.educore.sge.kindergarten.application.dto;

import com.educore.sge.kindergarten.infrastructure.entity.RelationshipType;

public class TutorAssignmentRequest {
    private String tutorId; // <-- Cambiar de Long a String
    private RelationshipType relationshipType;
    private Boolean isPrimary;

    public TutorAssignmentRequest() {}

    public String getTutorId() {
        return tutorId;
    }

    public void setTutorId(String tutorId) {
        this.tutorId = tutorId;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}