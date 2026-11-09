package com.educore.sge.institution.infrastructure.repository;


import com.educore.sge.institution.infrastructure.entity.ClassScheduleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassScheduleRepository extends JpaRepository<ClassScheduleJpaEntity, String> {
    List<ClassScheduleJpaEntity> findByTenantId(String tenantId);
}