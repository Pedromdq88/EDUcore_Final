package com.educore.sge.institution.web;

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

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
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

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR')")
    @PostMapping
    public ResponseEntity<?> addPickup(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @RequestBody AuthorizedPickupJpaEntity body) {

        if (institutionId == null || institutionId.isBlank()) {
            return ResponseEntity.badRequest().body("Falta cabecera X-Institution-Id.");
        }

        if (body.getBirthDate() != null) {
            int calculatedAge = Period.between(body.getBirthDate(), LocalDate.now()).getYears();
            body.setAge(calculatedAge);
        }

        if (body.getAge() == null || body.getAge() < 18) {
            return ResponseEntity.badRequest().body("La persona autorizada debe ser mayor de 18 años (Art. 154 Reglamento Gral.).");
        }

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

        if (body.getBirthDate() != null) {
            int calculatedAge = Period.between(body.getBirthDate(), LocalDate.now()).getYears();
            body.setAge(calculatedAge);
        }

        if (body.getAge() == null || body.getAge() < 18) {
            return ResponseEntity.badRequest().body("La persona autorizada debe ser mayor de 18 años.");
        }

        return repository.findById(pickupId)
                .map(existing -> {
                    if (!institutionId.equals(existing.getTenantId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No pertenece a esta institución.");
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

        return repository.findById(pickupId)
                .map(existing -> {
                    if (!institutionId.equals(existing.getTenantId())) {
                        return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
                    }
                    repository.delete(existing);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}