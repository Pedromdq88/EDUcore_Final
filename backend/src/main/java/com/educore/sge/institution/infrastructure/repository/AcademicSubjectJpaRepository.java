package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.AcademicSubjectJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicSubjectJpaRepository extends JpaRepository<AcademicSubjectJpaEntity, String> {
    List<AcademicSubjectJpaEntity> findAllByTenantIdOrderByNameAsc(String tenantId);
    Optional<AcademicSubjectJpaEntity> findByTenantIdAndNameIgnoreCase(String tenantId, String name);
}