package com.educore.sge.institution.web.school;

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

@RestController
@RequestMapping({"/api/v1/institutions", "/api/v1/institution"})
public class InstitutionController {

    private final InstitutionJpaRepository repository;
    private final com.educore.sge.auth.mail.EmailProofService emailVerification;

    public InstitutionController(InstitutionJpaRepository repository,
            com.educore.sge.auth.mail.EmailProofService emailVerification) {
        this.repository = repository;
        this.emailVerification = emailVerification;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
    @GetMapping
    public List<InstitutionJpaEntity> getAllInstitutions(@RequestHeader("X-Institution-Id") String tenantId) {
        return repository.findById(tenantId).map(List::of).orElse(List.of());
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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> updateInstitutionalEmails(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody Map<String, String> body,
            org.springframework.security.core.Authentication authentication) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la cabecera X-Institution-Id");
        }

        InstitutionJpaEntity institution = repository.findById(institutionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institución no encontrada"));

        // No guardar correos vacíos ni inválidos, aun si se llama la API directamente.
        String receiptEmail = com.educore.sge.shared.validation.RecordValidation.requiredEmail(
            body.get("receiptEmail"), "comprobantes");
        String queryEmail = com.educore.sge.shared.validation.RecordValidation.requiredEmail(
            body.get("feeQueryEmail"), "consultas de cuotas");
        String actor=authentication.getName();
        // Un correo que cambió solo puede guardarse si alguien pudo leer el código.
        emailVerification.requireProofForChange(institutionId,actor,institution.getReceiptEmail(),receiptEmail);
        emailVerification.requireProofForChange(institutionId,actor,institution.getFeeQueryEmail(),queryEmail);
        institution.setReceiptEmail(receiptEmail);
        institution.setFeeQueryEmail(queryEmail);
        repository.saveAndFlush(institution);
        emailVerification.consume(institutionId,actor,receiptEmail);
        emailVerification.consume(institutionId,actor,queryEmail);
        return ResponseEntity.ok().build();
    }
}