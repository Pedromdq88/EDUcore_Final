package com.educore.sge.institution.infrastructure.entity;

import com.educore.sge.institution.infrastructure.domain.ClassroomShift;
import com.educore.sge.shared.BaseInstitutionEntity;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "institution_classrooms_config")
public class ClassroomConfigJpaEntity extends BaseInstitutionEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name; // Ej: "1° Sección (3 años)" o "1° Año Primaria"

    @Column(name = "minimum_age", nullable = false)
    private Integer minimumAge; // Ej: 3 años requeridos al corte

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", length = 20, nullable = false)
    private ClassroomShift shift = ClassroomShift.MANANA;

}