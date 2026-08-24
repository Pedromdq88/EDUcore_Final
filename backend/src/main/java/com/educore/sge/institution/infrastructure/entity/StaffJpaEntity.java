package com.educore.sge.institution.infrastructure.entity;

import com.educore.sge.shared.BaseInstitutionEntity;
import com.educore.sge.shared.Rol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "institution_staff")
public class StaffJpaEntity extends BaseInstitutionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password = "123";

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Rol role;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "classroom", length = 100)
    private String classroom;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";
}