package com.educore.sge.institution.infrastructure.entity;

import com.educore.sge.shared.domain.EducationLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenants")
public class TenantJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "cue_code", length = 50)
    private String cueCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", length = 20, nullable = false)
    private EducationLevel educationLevel = EducationLevel.JARDIN;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}