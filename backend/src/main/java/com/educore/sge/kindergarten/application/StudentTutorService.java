package com.educore.sge.kindergarten.application;

import com.educore.sge.kindergarten.application.dto.TutorAssignmentRequest;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        // Un alumno nunca puede quedar registrado sin un tutor. La baja del último
        // tutor debe gestionarse por la operación de baja, que elimina también al alumno.
        if (tutorRequests == null || tutorRequests.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El alumno debe tener al menos un tutor; no se pueden quitar todos desde la edición.");
        }

        // 2. Regla de negocio: Máximo 2 tutores
        if (tutorRequests.size() > 2) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Un alumno de Jardín de Infantes no puede tener más de 2 tutores legales asignados.");
        }

        if (tutorRequests.stream().anyMatch(r -> r == null || r.getTutorId() == null || r.getTutorId().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccioná tutores válidos.");
        }
        // Si el frontend marca uno como principal, debe ser único.
        long requestedPrimaries = tutorRequests.stream().filter(r -> Boolean.TRUE.equals(r.getIsPrimary())).count();
        if (requestedPrimaries > 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Solo puede existir un tutor principal por alumno.");

        // 3. Validar no duplicidad de tutores
        long uniqueTutorsCount = tutorRequests.stream()
                .map(TutorAssignmentRequest::getTutorId)
                .distinct()
                .count();

        if (uniqueTutorsCount < tutorRequests.size()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "No se puede vincular el mismo tutor dos veces al mismo alumno.");
        }

        // 4. Buscar estudiante
        StudentJpaEntity existing = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
        // Serializar ediciones de tutores con bajas del tutor principal.
        StudentJpaEntity student = studentRepository.lockStudent(existing.getTenantId(), studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "La matrícula se modificó mientras se actualizaban los tutores."));

        // 5. Validar existencia de cada tutor en la BD y recopilarlos
        List<TutorJpaEntity> tutorsToAssign = new ArrayList<>();

        for (TutorAssignmentRequest request : tutorRequests) {
            TutorJpaEntity tutor = tutorRepository.findById(request.getTutorId())
                    .orElseThrow(() -> new RuntimeException("Tutor no encontrado con ID: " + request.getTutorId()));

            if (!student.getTenantId().equals(tutor.getTenantId())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Tutor de otra institución.");
            }
            tutorsToAssign.add(tutor);
        }

        // Conservar el principal anterior si todavía está vinculado y no se eligió otro.
        String chosen = tutorRequests.stream().filter(r -> Boolean.TRUE.equals(r.getIsPrimary()))
                .map(TutorAssignmentRequest::getTutorId).findFirst().orElse(null);
        if (chosen == null) {
            String previous = studentRepository.findPrimaryTutorId(studentId).orElse(null);
            chosen = tutorsToAssign.stream().map(TutorJpaEntity::getId)
                    .filter(id -> Objects.equals(id, previous)).findFirst().orElse(tutorsToAssign.get(0).getId());
        }
        // Primero guardar las relaciones; después marcar el principal con la columna existente.
        student.setTutors(tutorsToAssign);
        studentRepository.saveAndFlush(student);
        setPrimaryTutor(studentId, chosen);
    }

    /** Exactamente uno principal, con la columna `is_primary` que ya existe en MySQL. */
    @Transactional
    public void setPrimaryTutor(String studentId, String tutorId) {
        if (tutorId == null || !studentRepository.existsByIdAndTutorsId(studentId, tutorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tutor principal debe estar vinculado al alumno.");
        }
        studentRepository.clearPrimaryTutor(studentId);
        if (studentRepository.markPrimaryTutor(studentId, tutorId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo establecer el tutor principal.");
        }
    }

    /** Tras eliminar un tutor, el secundario pasa a principal automáticamente. */
    @Transactional
    public void ensurePrimaryAfterRemoval(String studentId, String removedTutorId,
                                          List<TutorJpaEntity> remaining) {
        if (remaining == null || remaining.isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un alumno activo no puede quedar sin tutor.");
        String previous = studentRepository.findPrimaryTutorId(studentId).orElse(null);
        String chosen = remaining.stream().map(TutorJpaEntity::getId)
                .filter(id -> !id.equals(removedTutorId) && id.equals(previous))
                .findFirst().orElse(remaining.get(0).getId());
        setPrimaryTutor(studentId, chosen);
    }


    public boolean isTutorOfStudent(String tutorEmail, String studentId) {
        // 1. Buscamos el tutor por su email de sesión
        TutorJpaEntity tutor = tutorRepository.findByEmail(tutorEmail).orElse(null);

        if (tutor == null) return false;

        // 2. Verificamos si existe la relación activa con el estudiante
        return studentRepository.existsByIdAndTutorsId(studentId, tutor.getId());
    }
}