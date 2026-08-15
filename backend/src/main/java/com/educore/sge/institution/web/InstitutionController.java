package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.InstitutionJpaEntity;
import com.educore.sge.institution.infrastructure.repository.InstitutionJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 1. GET: Consultar los correos institucionales
    @GetMapping("/settings/emails")
    public ResponseEntity<Map<String, String>> getInstitutionalEmails(
            @RequestHeader(value = "X-Institution-Id", required = false) String institutionId) {

        String targetId = (institutionId != null && !institutionId.isBlank())
                ? institutionId
                : "88888888-4444-4444-4444-121212121212";

        return repository.findById(targetId)
                .map(inst -> {
                    Map<String, String> emails = new HashMap<>();
                    emails.put("receiptEmail", inst.getReceiptEmail() != null ? inst.getReceiptEmail() : "administracion@onceunidos.com");
                    emails.put("feeQueryEmail", inst.getFeeQueryEmail() != null ? inst.getFeeQueryEmail() : "tesoreria@onceunidos.com");
                    return ResponseEntity.ok(emails);
                })
                .orElseGet(() -> {
                    Map<String, String> defaultEmails = new HashMap<>();
                    defaultEmails.put("receiptEmail", "administracion@onceunidos.com");
                    defaultEmails.put("feeQueryEmail", "tesoreria@onceunidos.com");
                    return ResponseEntity.ok(defaultEmails);
                });
    }

    // 2. PUT: Modificar y guardar los correos institucionales
    @PutMapping("/settings/emails")
    public ResponseEntity<Void> updateInstitutionalEmails(
            @RequestHeader(value = "X-Institution-Id", required = false) String institutionId,
            @RequestBody Map<String, String> body) {

        String targetId = (institutionId != null && !institutionId.isBlank())
                ? institutionId
                : "88888888-4444-4444-4444-121212121212";

        InstitutionJpaEntity institution = repository.findById(targetId)
                .orElseGet(() -> {
                    // Si no existe aún en la BD, se crea con el ID esperado
                    InstitutionJpaEntity nueva = new InstitutionJpaEntity();
                    nueva.setId(targetId);
                    nueva.setName("Jardín Once Unidos");
                    nueva.setSlug("jardin-once-unidos");
                    nueva.setStatus("ACTIVE");
                    return nueva;
                });

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