package com.educore.sge.academic.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "historial_alumnos_baja")
public class StudentHistoryJpaEntity {

    @Id
    private String id; // Mantenemos el mismo ID original para seguimiento

    private String firstName;
    private String lastName;
    private String documentNumber;
    private String classroom;
    private LocalDate birthDate;
    private String telefonoContacto;
    private String direccion;

    // El campo clave que registra el momento de la baja
    private LocalDate fechaBaja;

    // Constructores, Getters y Setters
    public StudentHistoryJpaEntity() {}

    public StudentHistoryJpaEntity(String id, String firstName, String lastName, String documentNumber, String classroom, LocalDate birthDate, String telefonoContacto, String direccion, LocalDate fechaBaja) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.documentNumber = documentNumber;
        this.classroom = classroom;
        this.birthDate = birthDate;
        this.telefonoContacto = telefonoContacto;
        this.direccion = direccion;
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

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public LocalDate getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(LocalDate fechaBaja) {
        this.fechaBaja = fechaBaja;
    }
}
