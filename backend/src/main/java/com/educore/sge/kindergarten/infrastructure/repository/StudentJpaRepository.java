package com.educore.sge.kindergarten.infrastructure.repository;

import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentJpaRepository extends JpaRepository<StudentJpaEntity, String> {

    // Trae los alumnos de una sala que NO estén dados de baja definitiva (para la lista de asistencia de la maestra)
    List<StudentJpaEntity> findByClassroomAndStatusNot(String classroom, StudentStatus status);

    // Trae sólo los alumnos que tienen una solicitud de baja esperando a ser aceptada (para el panel de la Directora)
    List<StudentJpaEntity> findByStatus(StudentStatus status);

    boolean existsByIdAndTutorsId(String id, String tutorsId);
}
