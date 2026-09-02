package com.educore.sge.institution.infrastructure.entity;

import com.educore.sge.shared.BaseInstitutionEntity;
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
@Table(name = "teacher_portfolios")
public class PortfolioJpaEntity extends BaseInstitutionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "staff_id", nullable = false, length = 36)
    private String staffId;

    @Column(name = "assignment_id", length = 36)
    private String assignmentId;

    @Column(name = "classroom", nullable = false, length = 100)
    private String classroom;

    @Column(name = "student_id", length = 36)
    private String studentId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    private String category = "PROYECTO";

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;
}