package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.AnnouncementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementJpaRepository extends JpaRepository<AnnouncementJpaEntity, String> {

    // Lista ordenada para la institución
    List<AnnouncementJpaEntity> findAllByTenantIdOrderByIsPinnedDescCreatedAtDesc(String tenantId);

    // Búsqueda específica filtrada para tutores y salitas
    @Query("SELECT a FROM AnnouncementJpaEntity a WHERE a.tenantId = :tenantId AND (" +
            "a.scope = 'GLOBAL' OR " +
            "(a.scope = 'CLASSROOM' AND a.targetClassroom = :classroom) OR " +
            "(a.scope = 'PRIVATE_STUDENT' AND a.targetStudentId IN :studentIds)) " +
            "ORDER BY a.isPinned DESC, a.createdAt DESC")
    List<AnnouncementJpaEntity> findVisibleForTutor(
            @Param("tenantId") String tenantId,
            @Param("classroom") String classroom,
            @Param("studentIds") List<String> studentIds
    );
}