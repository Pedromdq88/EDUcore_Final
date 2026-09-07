package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.AuthorizedPickupJpaEntity;
import com.educore.sge.institution.infrastructure.repository.AuthorizedPickupJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping("/api/v1/students/{studentId}/authorized-pickups")
public class AuthorizedPickupController {

    private final AuthorizedPickupJpaRepository repository;

    public AuthorizedPickupController(AuthorizedPickupJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuthorizedPickupJpaEntity> getPickups(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId) {
        return repository.findAllByTenantIdAndStudentId(institutionId, studentId);
    }

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