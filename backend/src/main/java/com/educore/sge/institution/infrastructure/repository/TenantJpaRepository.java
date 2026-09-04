package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.TenantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, String> {
}