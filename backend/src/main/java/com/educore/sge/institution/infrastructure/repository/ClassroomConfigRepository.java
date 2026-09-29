package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.ClassroomConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomConfigRepository extends JpaRepository<ClassroomConfigJpaEntity, String> {
    List<ClassroomConfigJpaEntity> findByTenantId(String tenantId);
}