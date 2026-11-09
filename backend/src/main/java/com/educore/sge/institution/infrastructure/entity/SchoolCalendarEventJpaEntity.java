package com.educore.sge.institution.infrastructure.entity;

import com.educore.sge.shared.BaseInstitutionEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "school_calendar_events")
@Getter @Setter
public class SchoolCalendarEventJpaEntity extends BaseInstitutionEntity {
    @Id @Column(length=36) private String id;
    @Column(nullable=false) private String title;
    @Column(nullable=false) private LocalDate date;
    @Column(nullable=false) private String category;
    @Column(nullable=false) private String section;
}


