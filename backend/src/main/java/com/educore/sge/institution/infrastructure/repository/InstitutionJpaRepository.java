package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.InstitutionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstitutionJpaRepository extends JpaRepository<InstitutionJpaEntity, String> {
}
