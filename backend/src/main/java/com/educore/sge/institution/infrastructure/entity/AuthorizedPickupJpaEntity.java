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
@Table(name = "student_authorized_pickups")
public class AuthorizedPickupJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "student_id", length = 36, nullable = false)
    private String studentId;

    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;

    @Column(name = "document_number", length = 50, nullable = false)
    private String documentNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false)
    private Integer age;

    @Column(length = 50, nullable = false)
    private String relationship;

    @Column(length = 50, nullable = false)
    private String phone;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}