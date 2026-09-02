package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.AcademicSubjectJpaEntity;
import com.educore.sge.institution.infrastructure.repository.AcademicSubjectJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE})
@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private final AcademicSubjectJpaRepository subjectRepository;

    public SubjectController(AcademicSubjectJpaRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @GetMapping
    public List<AcademicSubjectJpaEntity> getSubjects(
            @RequestHeader("X-Institution-Id") String institutionId) {
        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la cabecera X-Institution-Id");
        }
        return subjectRepository.findAllByTenantIdOrderByNameAsc(institutionId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping
    public AcademicSubjectJpaEntity createSubject(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody AcademicSubjectJpaEntity body) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la cabecera X-Institution-Id");
        }

        if (body.getName() == null || body.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la materia es obligatorio.");
        }

        String raw = body.getName().trim().toLowerCase();
        String formattedName = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);

        if (subjectRepository.findByTenantIdAndNameIgnoreCase(institutionId, formattedName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La materia '" + formattedName + "' ya se encuentra registrada.");
        }

        AcademicSubjectJpaEntity entity = new AcademicSubjectJpaEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setTenantId(institutionId);
        entity.setName(formattedName);

        return subjectRepository.save(entity);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @DeleteMapping("/{id}")
    public void deleteSubject(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la cabecera X-Institution-Id");
        }

        AcademicSubjectJpaEntity entity = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Materia no encontrada"));

        if (!institutionId.equals(entity.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar materias de otra institución.");
        }

        subjectRepository.delete(entity);
    }
}