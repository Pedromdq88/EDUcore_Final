package com.educore.sge.kindergarten.web;

import com.educore.sge.kindergarten.application.StudentTutorService;
import com.educore.sge.auth.AuthService;
import org.springframework.security.core.Authentication;
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
    private final AuthService authService;
    private final com.educore.sge.auth.RoleScopeService scopes;

    @Autowired
    private TutorHistoryRepository tutorHistoryRepository;

    @Autowired
    private StudentHistoryRepository studentHistoryRepository;

    @Autowired
    private StudentTutorService studentTutorService;

    public TutorController(TutorJpaRepository repository, StudentJpaRepository studentRepository, TutorService tutorService,
                           AuthService authService, com.educore.sge.auth.RoleScopeService scopes) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.tutorService = tutorService;
        this.authService = authService;
        this.scopes = scopes;
    }

    // LISTAR TUTORES ACTIVOS (Filtrado por Institución Activa)
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @GetMapping
    public List<TutorJpaEntity> getAllTutors(
            @RequestHeader("X-Institution-Id") String institutionId) {
        return repository.findByTenantId(institutionId);
    }

    /** Solo estudiantes vinculados a este tutor; evita descargar todos los alumnos para armar su perfil. */
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','TUTOR')")
    @Transactional(readOnly = true)
    public List<LinkedStudent> linkedStudents(@PathVariable String id,
            @RequestHeader("X-Institution-Id") String tenantId, Authentication authentication) {
        TutorJpaEntity tutor = repository.findById(id).filter(t -> tenantId.equals(t.getTenantId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor no encontrado"));
        boolean isTutor = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TUTOR"));
        if (isTutor && !id.equals(scopes.verifiedTutorId(authentication,tenantId)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No podés consultar los alumnos de otro tutor.");
        return studentRepository.findByTenantIdAndTutor(tenantId, id).stream()
            .map(s -> new LinkedStudent(s.getId(), s.getFirstName(), s.getLastName(), s.getDocumentNumber(), s.getClassroom()))
            .toList();
    }

    public record LinkedStudent(String id, String firstName, String lastName, String documentNumber, String classroom) {}

    // REGISTRAR NUEVO TUTOR (Exigiendo el Tenant ID)
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping
    @Transactional
    public TutorJpaEntity createTutor(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody TutorJpaEntity tutor) {

        if (tutor.getFirstName() == null || tutor.getFirstName().isBlank() ||
            tutor.getLastName() == null || tutor.getLastName().isBlank() || tutor.getRelationship() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nombre, apellido y vínculo son obligatorios.");
        }
        String dni = tutor.getDocumentNumber() == null ? "" : tutor.getDocumentNumber().replaceAll("[^0-9]", "");
        if (dni.length() < 6 || dni.length() > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El DNI debe tener entre 6 y 12 dígitos.");
        }
        boolean duplicate = repository.findByTenantId(institutionId).stream().anyMatch(existing ->
            dni.equals(existing.getDocumentNumber().replaceAll("[^0-9]", "")));
        if (duplicate) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un tutor con ese DNI. Vinculalo desde el buscador.");
        tutor.setDocumentNumber(dni);
        tutor.setId(UUID.randomUUID().toString());
        tutor.setTenantId(institutionId);
        TutorJpaEntity saved = repository.save(tutor);
        authService.provisionTutorAccount(saved, institutionId);
        return saved;
    }

    /** Activa cuenta para tutores creados antes de la integración, sin duplicarlos. */
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
    @PostMapping("/{id}/activate-account")
    @Transactional
    public java.util.Map<String,String> activateAccount(@PathVariable String id, @RequestHeader("X-Institution-Id") String tenantId) {
        TutorJpaEntity tutor = repository.findById(id)
          .filter(t -> tenantId.equals(t.getTenantId()))
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor no encontrado"));
        boolean created = authService.provisionTutorAccount(tutor, tenantId);
        return java.util.Map.of("status", created ? "CREATED" : "EXISTS", "email", tutor.getEmail());
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

        List<StudentJpaEntity> alumnos = studentRepository.findByTenantId(institutionId);
        // Nunca eliminamos al alumno cuando se da de baja al único tutor.
        for (StudentJpaEntity alumno : alumnos) {
            if (alumno.getTutors()!=null && alumno.getTutors().contains(tutor) && alumno.getTutors().size()==1)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Antes de la baja, vinculá otro responsable a " + alumno.getFirstName() + ".");
        }
        for (StudentJpaEntity alumno : alumnos) {
            if (alumno.getTutors()!=null && alumno.getTutors().remove(tutor)) studentRepository.save(alumno);
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

        authService.disableTutorAccount(institutionId, tutor.getId());
        repository.delete(tutor);
    }

    // ACTUALIZAR DATOS DEL TUTOR
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TUTOR')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<TutorDTO> updateTutor(
            @PathVariable String id,
            @RequestBody TutorDTO tutorDto,
            @RequestHeader("X-Institution-Id") String institutionId,
            Authentication authentication) {
        TutorJpaEntity current = repository.findById(id)
          .filter(t -> institutionId.equals(t.getTenantId()))
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor no encontrado"));
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TUTOR")) &&
            (current.getEmail() == null || !current.getEmail().equalsIgnoreCase(authentication.getName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo podés editar tu propio perfil.");
        }
        TutorDTO updated = tutorService.updateTutor(id, tutorDto);
        authService.syncTutorEmail(repository.findById(id).orElseThrow());
        return ResponseEntity.ok(updated);
    }
}