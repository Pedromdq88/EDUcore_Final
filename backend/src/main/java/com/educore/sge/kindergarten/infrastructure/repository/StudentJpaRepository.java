package com.educore.sge.kindergarten.infrastructure.repository;

import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentJpaRepository extends JpaRepository<StudentJpaEntity, String> {

    List<StudentJpaEntity> findByTenantId(String tenantId);
    @Query("SELECT DISTINCT s FROM StudentJpaEntity s JOIN s.tutors t WHERE s.tenantId = :tenantId AND t.id = :tutorId")
    List<StudentJpaEntity> findByTenantIdAndTutor(@Param("tenantId") String tenantId, @Param("tutorId") String tutorId);
    @Query("SELECT COUNT(s) > 0 FROM StudentJpaEntity s JOIN s.tutors t WHERE s.id = :studentId AND t.id = :tutorId")
    boolean existsByIdAndTutorsId(@Param("studentId") String studentId, @Param("tutorId") String tutorId);

    /** Existing student_tutors.is_primary column: no schema migration. */
    @Query(value = "SELECT tutor_id FROM student_tutors WHERE student_id = :studentId AND is_primary = 1 ORDER BY created_at, tutor_id LIMIT 1", nativeQuery = true)
    Optional<String> findPrimaryTutorId(@Param("studentId") String studentId);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE student_tutors SET is_primary = 0 WHERE student_id = :studentId", nativeQuery = true)
    int clearPrimaryTutor(@Param("studentId") String studentId);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE student_tutors SET is_primary = 1 WHERE student_id = :studentId AND tutor_id = :tutorId", nativeQuery = true)
    int markPrimaryTutor(@Param("studentId") String studentId, @Param("tutorId") String tutorId);

    /** A consistent parent row lock protects concurrent tutor removals for one student. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StudentJpaEntity s WHERE s.id = :studentId AND s.tenantId = :tenantId")
    Optional<StudentJpaEntity> lockStudent(@Param("tenantId") String tenantId, @Param("studentId") String studentId);

    /** Primary tutor IDs for Excel, lists and profiles; only within a tenant. */
    @Query(value = "SELECT st.student_id, st.tutor_id FROM student_tutors st JOIN students s ON s.id = st.student_id WHERE s.tenant_id = :tenantId AND st.is_primary = 1", nativeQuery = true)
    List<Object[]> findPrimaryTutorsByTenant(@Param("tenantId") String tenantId);

}