package com.educore.sge.institution.web.records;

import com.educore.sge.institution.infrastructure.entity.AuthorizedPickupJpaEntity;
import com.educore.sge.institution.infrastructure.repository.AuthorizedPickupJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students/{studentId}/authorized-pickups")
public class AuthorizedPickupController {

    private final AuthorizedPickupJpaRepository repository;
    private final com.educore.sge.auth.RoleScopeService scopes;
    private final com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students;

    public AuthorizedPickupController(AuthorizedPickupJpaRepository repository,
            com.educore.sge.auth.RoleScopeService scopes,
            com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students) {
        this.repository = repository;
        this.scopes = scopes;
        this.students = students;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR','TUTOR')")
    @GetMapping
    public List<AuthorizedPickupJpaEntity> getPickups(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            org.springframework.security.core.Authentication authentication) {
        var student = students.findById(studentId)
            .filter(s -> institutionId.equals(s.getTenantId()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Alumno no encontrado"));
        if (scopes.is(authentication, "TUTOR")) {
            String tutorId = scopes.verifiedTutorId(authentication, institutionId);
            if (!students.existsByIdAndTutorsId(student.getId(), tutorId)) {
                throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo podés consultar autorizados de tus hijos vinculados");
            }
        }
        return repository.findAllByTenantIdAndStudentId(institutionId, studentId);
    }

    private void checkStudent(String institutionId, String studentId) {
        students.findById(studentId).filter(s -> institutionId.equals(s.getTenantId()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Alumno no encontrado en esta institución."));
    }

    private void validatePickup(String institutionId, String studentId, String currentId, AuthorizedPickupJpaEntity body) {
        body.setFullName(com.educore.sge.shared.validation.RecordValidation.requiredText(body.getFullName(), "Nombre del autorizado", 150));
        body.setRelationship(com.educore.sge.shared.validation.RecordValidation.requiredText(body.getRelationship(), "Vínculo", 50));
        body.setPhone(com.educore.sge.shared.validation.RecordValidation.optionalPhone(
                com.educore.sge.shared.validation.RecordValidation.requiredText(body.getPhone(), "Teléfono", 50), "autorizado"));
        String dni = com.educore.sge.shared.validation.RecordValidation.requiredDni(body.getDocumentNumber(), "autorizado");
        body.setDocumentNumber(dni);
        if (body.getBirthDate() == null || body.getBirthDate().isAfter(LocalDate.now()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La fecha de nacimiento del autorizado es obligatoria y no puede ser futura.");
        body.setAge(Period.between(body.getBirthDate(), LocalDate.now()).getYears());
        if (body.getAge() < 18) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
            "La persona autorizada debe ser mayor de 18 años.");
        for (AuthorizedPickupJpaEntity other : repository.findAllByTenantIdAndStudentId(institutionId, studentId)) {
            if (other.getId().equals(currentId)) continue;
            if (dni.equals(com.educore.sge.shared.validation.RecordValidation.comparableDni(other.getDocumentNumber())))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta persona ya está autorizada para retirar a este alumno.");
        }
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR')")
    @PostMapping
    public ResponseEntity<?> addPickup(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @RequestBody AuthorizedPickupJpaEntity body) {

        if (institutionId == null || institutionId.isBlank()) {
            return ResponseEntity.badRequest().body("Falta cabecera X-Institution-Id.");
        }

        checkStudent(institutionId, studentId);
        validatePickup(institutionId, studentId, null, body);

        body.setId(UUID.randomUUID().toString());
        body.setTenantId(institutionId);
        body.setStudentId(studentId);
        return ResponseEntity.ok(repository.save(body));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR')")
    @PutMapping("/{pickupId}")
    public ResponseEntity<?> updatePickup(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @PathVariable String pickupId,
            @RequestBody AuthorizedPickupJpaEntity body) {

        checkStudent(institutionId, studentId);
        validatePickup(institutionId, studentId, pickupId, body);

        return repository.findById(pickupId)
                .map(existing -> {
                    if (!institutionId.equals(existing.getTenantId()) || !studentId.equals(existing.getStudentId())) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Autorización no encontrada para este alumno.");
                    }
                    existing.setFullName(body.getFullName());
                    existing.setDocumentNumber(body.getDocumentNumber());
                    existing.setBirthDate(body.getBirthDate());
                    existing.setAge(body.getAge());
                    existing.setRelationship(body.getRelationship());
                    existing.setPhone(body.getPhone());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR')")
    @DeleteMapping("/{pickupId}")
    public ResponseEntity<Void> deletePickup(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @PathVariable String pickupId) {

        checkStudent(institutionId, studentId);
        return repository.findById(pickupId)
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