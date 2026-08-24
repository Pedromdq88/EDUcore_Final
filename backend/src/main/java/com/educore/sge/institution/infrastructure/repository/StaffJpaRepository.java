package com.educore.sge.institution.infrastructure.repository;
import com.educore.sge.institution.infrastructure.entity.StaffJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffJpaRepository extends JpaRepository<StaffJpaEntity, String> {
    List<StaffJpaEntity> findAllByTenantId(String tenantId);
    Optional<StaffJpaEntity> findByEmail(String email);
    Optional<StaffJpaEntity> findByEmailAndTenantId(String email, String tenantId);

}
