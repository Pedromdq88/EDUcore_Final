package com.educore.sge.institution.infrastructure.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeJpaRepository extends JpaRepository<FeeJpaEntity, String> {

    // Consultas Multi-Tenant (con tenantId)
    List<FeeJpaEntity> findByTenantIdAndStudentIdAndAcademicYear(String tenantId, String studentId, Integer academicYear);

    Optional<FeeJpaEntity> findByTenantIdAndStudentIdAndAcademicYearAndMonthNumber(
            String tenantId, String studentId, Integer academicYear, Integer monthNumber);

    long countByTenantIdAndStudentIdAndAcademicYearAndStatus(
            String tenantId, String studentId, Integer academicYear, FeeStatus status);

    // Consultas históricas / directas por alumno
    List<FeeJpaEntity> findByStudentIdAndAcademicYear(String studentId, Integer academicYear);

    Optional<FeeJpaEntity> findByStudentIdAndAcademicYearAndMonthNumber(String studentId, Integer academicYear, Integer monthNumber);

    long countByStudentIdAndAcademicYearAndStatus(String studentId, Integer academicYear, FeeStatus status);
}