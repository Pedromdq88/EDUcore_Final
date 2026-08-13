package com.educore.sge.kindergarten.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorDTO {

    private String id;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String relationship;
    private String phone;
    private String email;
    private String direccion;

    // --- CAMPOS NUEVOS ---
    private String nacionalidad;
    private String profesion;
    private String condicionActividad;
    private String convive;
}