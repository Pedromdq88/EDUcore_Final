package com.educore.sge.kindergarten.infrastructure.repository;

import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentJpaRepository extends JpaRepository<StudentJpaEntity, String> {

    List<StudentJpaEntity> findByTenantId(String tenantId);
    @Query("SELECT COUNT(s) > 0 FROM StudentJpaEntity s JOIN s.tutors t WHERE s.id = :studentId AND t.id = :tutorId")
    boolean existsByIdAndTutorsId(@Param("studentId") String studentId, @Param("tutorId") String tutorId);
}