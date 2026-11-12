package com.educore.sge.institution.web.records;

import com.educore.sge.institution.infrastructure.entity.JudicialRestrictionJpaEntity;
import com.educore.sge.institution.infrastructure.repository.JudicialRestrictionJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students/{studentId}/judicial-restrictions")
public class JudicialRestrictionController {

    private final JudicialRestrictionJpaRepository repository;
    private final com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students;

    public JudicialRestrictionController(JudicialRestrictionJpaRepository repository,
            com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students) {
        this.repository = repository;
        this.students = students;
    }

    private void checkStudent(String institutionId, String studentId) {
        students.findById(studentId).filter(s -> institutionId.equals(s.getTenantId()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Alumno no encontrado en esta institución."));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR')")
    @GetMapping
    public List<JudicialRestrictionJpaEntity> getRestrictions(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId) {
        checkStudent(institutionId, studentId);
        return repository.findAllByTenantIdAndStudentId(institutionId, studentId);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
    @PostMapping
    public ResponseEntity<?> addRestriction(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @RequestBody JudicialRestrictionJpaEntity body) {

        if (institutionId == null || institutionId.isBlank()) {
            return ResponseEntity.badRequest().body("Falta cabecera X-Institution-Id.");
        }

        checkStudent(institutionId, studentId);
        body.setFirstName(com.educore.sge.shared.validation.RecordValidation.requiredText(body.getFirstName(), "Nombre", 100));
        body.setLastName(com.educore.sge.shared.validation.RecordValidation.requiredText(body.getLastName(), "Apellido", 100));
        body.setDocumentNumber(com.educore.sge.shared.validation.RecordValidation.requiredDni(body.getDocumentNumber(), "persona restringida"));
        body.setDocumentType(com.educore.sge.shared.validation.RecordValidation.requiredText(body.getDocumentType(), "Tipo de documento", 50));
        body.setDescription(com.educore.sge.shared.validation.RecordValidation.requiredText(body.getDescription(), "Motivo de restricción", 4000));
        body.setId(UUID.randomUUID().toString());
        body.setTenantId(institutionId);
        body.setStudentId(studentId);

        return ResponseEntity.ok(repository.save(body));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
    @DeleteMapping("/{restrictionId}")
    public ResponseEntity<Void> deleteRestriction(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @PathVariable String restrictionId) {

        checkStudent(institutionId, studentId);
        return repository.findById(restrictionId)
                .map(existing -> {
                    if (!institutionId.equals(existing.getTenantId()) || !studentId.equals(existing.getStudentId())) {
                        return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
                    }
                    repository.delete(existing);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}