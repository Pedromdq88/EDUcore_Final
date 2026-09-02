package com.educore.sge.institution.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "staff_history")
public class StaffHistoryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "classroom", length = 100)
    private String classroom;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    // 👈 AQUÍ: Mapear explícitamente a baja_date
    @Column(name = "baja_date", nullable = false)
    private LocalDate fechaBaja;

    // Constructor que usa tu StaffController
    public StaffHistoryJpaEntity(String id, String tenantId, String firstName, String lastName,
                                 String email, String role, LocalDate hireDate, LocalDate fechaBaja) {
        this.id = id;
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.hireDate = hireDate;
        this.fechaBaja = fechaBaja;
    }
}