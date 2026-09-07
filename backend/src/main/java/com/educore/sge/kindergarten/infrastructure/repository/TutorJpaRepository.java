package com.educore.sge.kindergarten.infrastructure.repository;

import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // 👈 Cambiado java.lang.ScopedValue por java.util.Optional

@Repository
public interface TutorJpaRepository extends JpaRepository<TutorJpaEntity, String> {
    List<TutorJpaEntity> findByTenantId(String tenantId);
    Optional<TutorJpaEntity> findByEmail(String email);
}