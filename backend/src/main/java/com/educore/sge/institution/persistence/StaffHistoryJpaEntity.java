package com.educore.sge.institution.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@NoArgsConstructor
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
    private LocalDate hireDate; ///Fecha de contratacion
    private LocalDate fechaBaja;


    public StaffHistoryJpaEntity(String id, String firstName, String lastName, String email, String role, LocalDate hireDate, LocalDate bajaDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.hireDate = hireDate;
        this.fechaBaja = bajaDate;
    }

}
