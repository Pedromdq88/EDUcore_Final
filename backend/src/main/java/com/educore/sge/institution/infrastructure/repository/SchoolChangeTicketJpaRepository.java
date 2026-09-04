package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.SchoolChangeTicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SchoolChangeTicketJpaRepository extends JpaRepository<SchoolChangeTicketJpaEntity, String> {
    List<SchoolChangeTicketJpaEntity> findAllByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);
}