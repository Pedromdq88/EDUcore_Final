package com.educore.sge.institution.infrastructure.entity;

import com.educore.sge.shared.BaseInstitutionEntity;
import com.educore.sge.shared.Shift;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "staff")
@ToString(exclude = "staff")
@Entity
@Table(name = "staff_assignments")
public class StaffAssignmentJpaEntity extends BaseInstitutionEntity {

    @Id
    @Column(name = "id",updatable = false, nullable = false, length = 40)
    private String id;

    @Column(name = "subject", nullable = false ,length = 100)
    private String subject;

    @Column(name = "classroom",nullable = false,length = 100)
    private String classroom;

    @Column(name = "is_classroomTeacher",nullable = false)
    private Boolean isClassroomTeacher = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", nullable = false, length = 50)
    private Shift shift = Shift.MANANA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id",nullable = false)
    @JsonBackReference
    private StaffJpaEntity staff;

}
