package com.educore.sge.institution.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "historial_personal_baja")
public class StaffHistoryJpaEntity {

    @Id
    private String id;

    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String classroom;
    private LocalDate hireDate;

    private LocalDate fechaBaja;

    public StaffHistoryJpaEntity() {}

    public StaffHistoryJpaEntity(String id, String firstName, String lastName, String email, String role, String classroom, LocalDate hireDate, LocalDate fechaBaja) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.classroom = classroom;
        this.hireDate = hireDate;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public LocalDate getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(LocalDate fechaBaja) {
        this.fechaBaja = fechaBaja;
    }
}
