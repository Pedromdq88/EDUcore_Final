package com.educore.sge.institution.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_judicial_restrictions")
public class JudicialRestrictionJpaEntity {

    @Id
    private String id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "legajo_number")
    private String legajoNumber;

    @Column(name = "matrix_number")
    private String matrixNumber;

    @Column(name = "folio_number")
    private String folioNumber;

    @Column(name = "inscription_date")
    private LocalDate inscriptionDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}