package com.educore.sge.kindergarten.application.dto;

import com.educore.sge.kindergarten.infrastructure.entity.RelationshipType;

public class TutorSummaryDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private RelationshipType relationshipType;

    public TutorSummaryDto() {
    }

    public TutorSummaryDto(Long id, String firstName, String lastName, String phone, RelationshipType relationshipType) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.relationshipType = relationshipType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }
}