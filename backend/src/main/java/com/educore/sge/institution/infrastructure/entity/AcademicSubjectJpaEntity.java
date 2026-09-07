package com.educore.sge.institution.infrastructure.entity;

import com.educore.sge.shared.BaseInstitutionEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "academic_subjects")
public class AcademicSubjectJpaEntity extends BaseInstitutionEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "name", nullable = false,length = 100)
    private String name;
}
