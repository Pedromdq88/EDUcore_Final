package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.SchoolProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface SchoolProfileJpaRepository extends JpaRepository<SchoolProfileJpaEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SchoolProfileJpaEntity p where p.tenantId = :tenantId")
    Optional<SchoolProfileJpaEntity> findForUpdate(@Param("tenantId") String tenantId);
}