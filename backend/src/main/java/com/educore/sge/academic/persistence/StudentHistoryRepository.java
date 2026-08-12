package com.educore.sge.academic.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentHistoryRepository extends JpaRepository<StudentHistoryJpaEntity, String> {
}
