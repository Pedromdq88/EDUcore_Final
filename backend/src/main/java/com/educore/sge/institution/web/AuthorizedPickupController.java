package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.AuthorizedPickupJpaEntity;
import com.educore.sge.institution.infrastructure.repository.AuthorizedPickupJpaRepository;
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
    public List<AuthorizedPickupJpaEntity> getPickups(@PathVariable String studentId) {
        return repository.findByStudentId(studentId);
    }

    @PostMapping
    public ResponseEntity<?> addPickup(@PathVariable String studentId, @RequestBody AuthorizedPickupJpaEntity body) {
        if (body.getBirthDate() != null) {
            int calculatedAge = Period.between(body.getBirthDate(), LocalDate.now()).getYears();
            body.setAge(calculatedAge);
        }

        if (body.getAge() == null || body.getAge() < 18) {
            return ResponseEntity.badRequest().body("La persona autorizada debe ser mayor de 18 años (Art. 154 Reglamento Gral.).");
        }

        body.setId(UUID.randomUUID().toString());
        body.setStudentId(studentId);
        return ResponseEntity.ok(repository.save(body));
    }

    @PutMapping("/{pickupId}")
    public ResponseEntity<?> updatePickup(
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
    public ResponseEntity<Void> deletePickup(@PathVariable String studentId, @PathVariable String pickupId) {
        if (repository.existsById(pickupId)) {
            repository.deleteById(pickupId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}