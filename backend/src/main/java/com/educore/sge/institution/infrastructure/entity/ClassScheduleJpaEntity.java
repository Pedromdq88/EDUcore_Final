package com.educore.sge.institution.infrastructure.entity;
import com.educore.sge.shared.BaseInstitutionEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

    @Entity
    @Table(name = "class_schedules")
    @Getter @Setter
    public class ClassScheduleJpaEntity extends BaseInstitutionEntity {
        @Id @Column(length = 36)
        private String id;
        @Column(name = "classroom_id", nullable = false, length = 36)
        private String classroomId;
        @Column(nullable = false)
        private String subject;
        @Column(name = "teacher_name", nullable = false)
        private String teacherName;
        @Column(name = "day_of_week", nullable = false)
        private Integer dayOfWeek;
        @Column(name = "start_time", nullable = false)
        private LocalTime startTime;
        @Column(name = "end_time", nullable = false)
        private LocalTime endTime;
    }




