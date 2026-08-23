package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.AuthorizedPickupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuthorizedPickupJpaRepository extends JpaRepository<AuthorizedPickupJpaEntity, String> {
    List<AuthorizedPickupJpaEntity> findByStudentId(String studentId);
}