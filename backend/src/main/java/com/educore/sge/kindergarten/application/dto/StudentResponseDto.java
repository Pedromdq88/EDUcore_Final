package com.educore.sge.kindergarten.application.dto;

import com.educore.sge.kindergarten.infrastructure.entity.StudentStatus;
import java.time.LocalDate;
import java.util.List;

public class StudentResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private LocalDate birthDate;
    private String classroom;
    private String direccion;
    private StudentStatus status;

    // Contactos de emergencia derivados automáticamente de los tutores
    private String telefonoContacto1;
    private String nombreContacto1;
    private String parentescoContacto1;

    private String telefonoContacto2;
    private String nombreContacto2;
    private String parentescoContacto2;

    private List<TutorSummaryDto> tutors;

    // Métodos utilitarios para mapear desde los tutores
    public void populateEmergencyContacts(List<TutorSummaryDto> assignedTutors) {
        if (assignedTutors != null && !assignedTutors.isEmpty()) {
            // Tutor 1 (Principal)
            TutorSummaryDto t1 = assignedTutors.get(0);
            this.nombreContacto1 = t1.getFirstName() + " " + t1.getLastName();
            this.telefonoContacto1 = t1.getPhone();
            this.parentescoContacto1 = t1.getRelationshipType() != null ? t1.getRelationshipType().name() : null;

            // Tutor 2 (Secundario, si existe)
            if (assignedTutors.size() > 1) {
                TutorSummaryDto t2 = assignedTutors.get(1);
                this.nombreContacto2 = t2.getFirstName() + " " + t2.getLastName();
                this.telefonoContacto2 = t2.getPhone();
                this.parentescoContacto2 = t2.getRelationshipType() != null ? t2.getRelationshipType().name() : null;
            }
        }
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

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public String getTelefonoContacto1() {
        return telefonoContacto1;
    }

    public void setTelefonoContacto1(String telefonoContacto1) {
        this.telefonoContacto1 = telefonoContacto1;
    }

    public String getNombreContacto1() {
        return nombreContacto1;
    }

    public void setNombreContacto1(String nombreContacto1) {
        this.nombreContacto1 = nombreContacto1;
    }

    public String getParentescoContacto1() {
        return parentescoContacto1;
    }

    public void setParentescoContacto1(String parentescoContacto1) {
        this.parentescoContacto1 = parentescoContacto1;
    }

    public String getTelefonoContacto2() {
        return telefonoContacto2;
    }

    public void setTelefonoContacto2(String telefonoContacto2) {
        this.telefonoContacto2 = telefonoContacto2;
    }

    public String getNombreContacto2() {
        return nombreContacto2;
    }

    public void setNombreContacto2(String nombreContacto2) {
        this.nombreContacto2 = nombreContacto2;
    }

    public String getParentescoContacto2() {
        return parentescoContacto2;
    }

    public void setParentescoContacto2(String parentescoContacto2) {
        this.parentescoContacto2 = parentescoContacto2;
    }

    public List<TutorSummaryDto> getTutors() {
        return tutors;
    }

    public void setTutors(List<TutorSummaryDto> tutors) {
        this.tutors = tutors;
    }
}