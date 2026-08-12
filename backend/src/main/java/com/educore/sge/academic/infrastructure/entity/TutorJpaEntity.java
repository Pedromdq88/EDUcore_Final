package com.educore.sge.academic.infrastructure.entity;

import com.educore.sge.shared.BaseInstitutionEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "tutors")
public class TutorJpaEntity extends BaseInstitutionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false)
    private RelationshipType relationship;

    public TutorJpaEntity() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public RelationshipType getRelationship() { return relationship; }
    public void setRelationship(RelationshipType relationship) { this.relationship = relationship; }
}
