package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.PortfolioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioJpaRepository extends JpaRepository<PortfolioJpaEntity, String> {

    List<PortfolioJpaEntity> findAllByTenantIdOrderByActivityDateDesc(String tenantId);

    List<PortfolioJpaEntity> findAllByTenantIdAndStaffIdOrderByActivityDateDesc(String tenantId, String staffId);

    List<PortfolioJpaEntity> findAllByTenantIdAndClassroomOrderByActivityDateDesc(String tenantId, String classroom);

    List<PortfolioJpaEntity> findAllByTenantIdAndStudentIdOrderByActivityDateDesc(String tenantId, String studentId);
}