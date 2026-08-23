package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.JudicialRestrictionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JudicialRestrictionJpaRepository extends JpaRepository<JudicialRestrictionJpaEntity, String> {
    List<JudicialRestrictionJpaEntity> findByStudentId(String studentId);
}