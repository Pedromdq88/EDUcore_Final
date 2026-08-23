package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.JudicialRestrictionJpaEntity;
import com.educore.sge.institution.infrastructure.repository.JudicialRestrictionJpaRepository;
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
    public List<JudicialRestrictionJpaEntity> getRestrictions(@PathVariable String studentId) {
        return repository.findByStudentId(studentId);
    }

    @PostMapping
    public ResponseEntity<JudicialRestrictionJpaEntity> addRestriction(
            @PathVariable String studentId,
            @RequestBody JudicialRestrictionJpaEntity body) {
        body.setId(UUID.randomUUID().toString());
        body.setStudentId(studentId);
        return ResponseEntity.ok(repository.save(body));
    }

    @PutMapping("/{restrictionId}")
    public ResponseEntity<JudicialRestrictionJpaEntity> updateRestriction(
            @PathVariable String studentId,
            @PathVariable String restrictionId,
            @RequestBody JudicialRestrictionJpaEntity body) {
        return repository.findById(restrictionId)
                .map(existing -> {
                    existing.setLastName(body.getLastName());
                    existing.setFirstName(body.getFirstName());
                    existing.setDocumentType(body.getDocumentType());
                    existing.setDocumentNumber(body.getDocumentNumber());
                    existing.setDescription(body.getDescription());
                    existing.setLegajoNumber(body.getLegajoNumber());
                    existing.setMatrixNumber(body.getMatrixNumber());
                    existing.setFolioNumber(body.getFolioNumber());
                    existing.setInscriptionDate(body.getInscriptionDate());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{restrictionId}")
    public ResponseEntity<Void> deleteRestriction(
            @PathVariable String studentId,
            @PathVariable String restrictionId) {
        if (repository.existsById(restrictionId)) {
            repository.deleteById(restrictionId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}