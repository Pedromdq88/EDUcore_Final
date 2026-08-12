package com.educore.sge.institution.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffHistoryRepository extends JpaRepository<StaffHistoryJpaEntity, String> {
}
