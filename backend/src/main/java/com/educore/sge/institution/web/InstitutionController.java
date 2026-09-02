package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.InstitutionJpaEntity;
import com.educore.sge.institution.infrastructure.repository.InstitutionJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping({"/api/v1/institutions", "/api/v1/institution"})
public class InstitutionController {

    private final InstitutionJpaRepository repository;

    public InstitutionController(InstitutionJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<InstitutionJpaEntity> getAllInstitutions() {
        return repository.findAll();
    }

    // 1. GET: Consultar correos oficiales de la institución activa
    @GetMapping("/settings/emails")
    public ResponseEntity<Map<String, String>> getInstitutionalEmails(
            @RequestHeader("X-Institution-Id") String institutionId) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la cabecera X-Institution-Id");
        }

        InstitutionJpaEntity inst = repository.findById(institutionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institución no encontrada"));

        Map<String, String> emails = new HashMap<>();
        emails.put("receiptEmail", inst.getReceiptEmail() != null ? inst.getReceiptEmail() : "");
        emails.put("feeQueryEmail", inst.getFeeQueryEmail() != null ? inst.getFeeQueryEmail() : "");

        return ResponseEntity.ok(emails);
    }

    // 2. PUT: Modificar y guardar los correos de la institución activa
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PutMapping("/settings/emails")
    public ResponseEntity<Void> updateInstitutionalEmails(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody Map<String, String> body) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la cabecera X-Institution-Id");
        }

        InstitutionJpaEntity institution = repository.findById(institutionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institución no encontrada"));

        if (body.containsKey("receiptEmail")) {
            institution.setReceiptEmail(body.get("receiptEmail"));
        }
        if (body.containsKey("feeQueryEmail")) {
            institution.setFeeQueryEmail(body.get("feeQueryEmail"));
        }

        repository.save(institution);
        return ResponseEntity.ok().build();
    }
}