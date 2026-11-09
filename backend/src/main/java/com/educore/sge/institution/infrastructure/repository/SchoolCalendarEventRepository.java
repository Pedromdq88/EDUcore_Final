package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.SchoolCalendarEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SchoolCalendarEventRepository extends JpaRepository<SchoolCalendarEventJpaEntity,String> {
    List<SchoolCalendarEventJpaEntity> findByTenantId(String tenantId);
}