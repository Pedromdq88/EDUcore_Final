package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.JudicialRestrictionJpaEntity;
import com.educore.sge.institution.infrastructure.repository.JudicialRestrictionJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping("/api/v1/students/{studentId}/judicial-restrictions")
public class JudicialRestrictionController {

    private final JudicialRestrictionJpaRepository repository;

    public JudicialRestrictionController(JudicialRestrictionJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<JudicialRestrictionJpaEntity> getRestrictions(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId) {
        return repository.findAllByTenantIdAndStudentId(institutionId, studentId);
    }

    @PostMapping
    public ResponseEntity<?> addRestriction(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @RequestBody JudicialRestrictionJpaEntity body) {

        if (institutionId == null || institutionId.isBlank()) {
            return ResponseEntity.badRequest().body("Falta cabecera X-Institution-Id.");
        }

        body.setId(UUID.randomUUID().toString());
        body.setTenantId(institutionId);
        body.setStudentId(studentId);

        return ResponseEntity.ok(repository.save(body));
    }

    @DeleteMapping("/{restrictionId}")
    public ResponseEntity<Void> deleteRestriction(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @PathVariable String restrictionId) {

        return repository.findById(restrictionId)
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