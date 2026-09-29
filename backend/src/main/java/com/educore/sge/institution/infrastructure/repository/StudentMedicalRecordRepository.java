package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.StudentMedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentMedicalRecordRepository extends JpaRepository<StudentMedicalRecord, String> {
    Optional<StudentMedicalRecord> findByStudentIdAndTenantId(String studentId, String tenantId);
}