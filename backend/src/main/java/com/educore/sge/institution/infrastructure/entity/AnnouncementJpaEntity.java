package com.educore.sge.institution.infrastructure.entity;

import com.educore.sge.shared.BaseInstitutionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "institution_announcements")
public class AnnouncementJpaEntity extends BaseInstitutionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "author_role", nullable = false)
    private String authorRole;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "category", nullable = false)
    private String category; // GENERAL, ACTIVIDAD, URGENTE, PRIVADO

    @Column(name = "scope", nullable = false)
    private String scope; // GLOBAL, CLASSROOM, PRIVATE_STUDENT

    @Column(name = "target_classroom")
    private String targetClassroom;

    @Column(name = "target_student_id")
    private String targetStudentId;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}