package com.educore.sge.academic.infrastructure.repository;

import com.educore.sge.academic.infrastructure.entity.TutorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorJpaRepository extends JpaRepository<TutorJpaEntity, String> {
}
