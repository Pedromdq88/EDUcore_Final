package com.educore.sge.kindergarten.web;

import com.educore.sge.kindergarten.application.StudentTutorService;
import com.educore.sge.kindergarten.application.TutorService;
import com.educore.sge.kindergarten.infrastructure.dto.TutorDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.persistence.TutorHistoryJpaEntity;
import com.educore.sge.kindergarten.persistence.TutorHistoryRepository;
import com.educore.sge.kindergarten.persistence.StudentHistoryJpaEntity;
import com.educore.sge.kindergarten.persistence.StudentHistoryRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/tutors")
public class TutorController {

    private final TutorJpaRepository repository;
    private final StudentJpaRepository studentRepository;
    private final TutorService tutorService;

    @Autowired
    private TutorHistoryRepository tutorHistoryRepository;

    @Autowired
    private StudentHistoryRepository studentHistoryRepository;

    @Autowired
    private StudentTutorService studentTutorService;

    public TutorController(TutorJpaRepository repository, StudentJpaRepository studentRepository, TutorService tutorService) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.tutorService = tutorService;
    }

    // LISTAR TUTORES ACTIVOS (Filtrado por Institución Activa)
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @GetMapping
    public List<TutorJpaEntity> getAllTutors(
            @RequestHeader("X-Institution-Id") String institutionId) {
        return repository.findByTenantId(institutionId);
    }

    // REGISTRAR NUEVO TUTOR (Exigiendo el Tenant ID)
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @PostMapping
    public TutorJpaEntity createTutor(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody TutorJpaEntity tutor) {

        tutor.setId(UUID.randomUUID().toString());
        tutor.setTenantId(institutionId);
        return repository.save(tutor);
    }

    // PROCESAR BAJA HISTÓRICA DEL TUTOR (Aislado por Tenant)
    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{id}/baja")
    public void darDeBajaTutor(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {

        TutorJpaEntity tutor = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor no encontrado"));

        if (!institutionId.equals(tutor.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre tutores de otra institución.");
        }

        // 🟢 Restringir la búsqueda únicamente a los alumnos del tenant activo
        List<StudentJpaEntity> todosLosAlumnos = studentRepository.findByTenantId(institutionId);
        List<StudentJpaEntity> alumnosAEliminar = new ArrayList<>();

        for (StudentJpaEntity alumno : todosLosAlumnos) {
            if (alumno.getTutors() != null && alumno.getTutors().contains(tutor)) {
                if (alumno.getTutors().size() <= 1) {
                    StudentHistoryJpaEntity alumnoHistorico = new StudentHistoryJpaEntity(
                            alumno.getId(),
                            alumno.getFirstName(),
                            alumno.getLastName(),
                            alumno.getDocumentNumber(),
                            alumno.getClassroom(),
                            alumno.getBirthDate(),
                            alumno.getContactPhone(),
                            alumno.getAddress(),
                            LocalDate.now()
                    );
                    studentHistoryRepository.save(alumnoHistorico);
                    alumnosAEliminar.add(alumno);
                } else {
                    alumno.getTutors().remove(tutor);
                    studentRepository.save(alumno);
                }
            }
        }

        for (StudentJpaEntity alumno : alumnosAEliminar) {
            alumno.getTutors().clear();
            studentRepository.save(alumno);
            studentRepository.delete(alumno);
        }

        TutorHistoryJpaEntity historico = new TutorHistoryJpaEntity(
                tutor.getId(),
                tutor.getFirstName(),
                tutor.getLastName(),
                tutor.getDocumentNumber(),
                tutor.getRelationship().name(),
                tutor.getPhone(),
                tutor.getEmail(),
                LocalDate.now()
        );
        tutorHistoryRepository.save(historico);

        for (StudentJpaEntity alumno : todosLosAlumnos) {
            if (alumno.getTutors() != null) {
                alumno.getTutors().remove(tutor);
            }
        }

        repository.delete(tutor);
    }

    // ACTUALIZAR DATOS DEL TUTOR
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER', 'TUTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<TutorDTO> updateTutor(
            @PathVariable String id,
            @RequestBody TutorDTO tutorDto,
            @RequestHeader("X-Institution-Id") String institutionId) {

        TutorDTO updated = tutorService.updateTutor(id, tutorDto);
        return ResponseEntity.ok(updated);
    }
}