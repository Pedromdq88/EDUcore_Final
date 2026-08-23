package com.educore.sge.institution.infrastructure.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeJpaRepository extends JpaRepository<FeeJpaEntity, String> {

    List<FeeJpaEntity> findByStudentIdAndAcademicYear(String studentId, Integer academicYear);

    Optional<FeeJpaEntity> findByStudentIdAndAcademicYearAndMonthNumber(String studentId, Integer academicYear, Integer monthNumber);

    long countByStudentIdAndAcademicYearAndStatus(String studentId, Integer academicYear, FeeStatus status);


}