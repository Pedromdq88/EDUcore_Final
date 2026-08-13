package com.educore.sge.kindergarten.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "historial_tutores_baja")
public class TutorHistoryJpaEntity {

    @Id
    private String id;

    private String firstName;
    private String lastName;
    private String documentNumber;
    private String relationship;
    private String phone;
    private String email;

    private LocalDate fechaBaja;

    public TutorHistoryJpaEntity() {}

    public TutorHistoryJpaEntity(String id, String firstName, String lastName, String documentNumber, String relationship, String phone, String email, LocalDate fechaBaja) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.documentNumber = documentNumber;
        this.relationship = relationship;
        this.phone = phone;
        this.email = email;
        this.fechaBaja = fechaBaja;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(LocalDate fechaBaja) {
        this.fechaBaja = fechaBaja;
    }
}
