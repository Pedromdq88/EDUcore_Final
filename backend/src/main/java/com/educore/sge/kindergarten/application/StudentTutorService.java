package com.educore.sge.kindergarten.application;

import com.educore.sge.kindergarten.application.dto.TutorAssignmentRequest;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentTutorService {

    private final StudentJpaRepository studentRepository;
    private final TutorJpaRepository tutorRepository;

    public StudentTutorService(StudentJpaRepository studentRepository, TutorJpaRepository tutorRepository) {
        this.studentRepository = studentRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public void assignTutorsToStudent(String studentId, List<TutorAssignmentRequest> tutorRequests) {
        // 1. Validar presencia de al menos 1 tutor
        if (tutorRequests == null || tutorRequests.isEmpty()) {
            throw new IllegalArgumentException("Un alumno de Jardín debe contar con al menos 1 tutor responsable.");
        }

        // 2. Regla de negocio: Máximo 2 tutores
        if (tutorRequests.size() > 2) {
            throw new IllegalArgumentException("Un alumno de Jardín de Infantes no puede tener más de 2 tutores legales asignados.");
        }

        // 3. Validar no duplicidad de tutores
        long uniqueTutorsCount = tutorRequests.stream()
                .map(TutorAssignmentRequest::getTutorId)
                .distinct()
                .count();

        if (uniqueTutorsCount < tutorRequests.size()) {
            throw new IllegalArgumentException("No se puede vincular el mismo tutor dos veces al mismo alumno.");
        }

        // 4. Buscar estudiante
        StudentJpaEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con ID: " + studentId));

        // 5. Validar existencia de cada tutor en la BD y recopilarlos
        List<TutorJpaEntity> tutorsToAssign = new ArrayList<>();

        for (TutorAssignmentRequest request : tutorRequests) {
            TutorJpaEntity tutor = tutorRepository.findById(request.getTutorId())
                    .orElseThrow(() -> new RuntimeException("Tutor no encontrado con ID: " + request.getTutorId()));

            tutorsToAssign.add(tutor);
        }

        // 6. Asignar la lista al estudiante y guardar
        student.setTutors(tutorsToAssign);
        studentRepository.save(student);


    }

    public boolean isTutorOfStudent(String tutorEmail, String studentId) {
        // 1. Buscamos el tutor por su email de sesión
        TutorJpaEntity tutor = tutorRepository.findByEmail(tutorEmail).orElse(null);

        if (tutor == null) return false;

        // 2. Verificamos si existe la relación activa con el estudiante
        return studentRepository.existsByIdAndTutorsId(studentId, tutor.getId());
    }
}