package com.educore.sge.institution.web.records;

import com.educore.sge.institution.infrastructure.entity.JudicialRestrictionJpaEntity;
import com.educore.sge.institution.infrastructure.repository.JudicialRestrictionJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.educore.sge.shared.validation.RecordValidation;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
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
        body.setFirstName(personName(body.getFirstName(), "Nombre"));
        body.setLastName(personName(body.getLastName(), "Apellido"));
        if (body.getDocumentNumber() == null || !body.getDocumentNumber().matches("[0-9]{6,8}")) {
            throw invalidField("DNI: ingresá entre 6 y 8 números, sin puntos ni espacios.");
        }
        String documentType = RecordValidation.requiredText(body.getDocumentType(), "Tipo de documento", 20);
        if (!documentType.matches("[\\p{L}][\\p{L}\\p{M}0-9 ./-]*")) {
            throw invalidField("Tipo de documento: ingresá un tipo válido, por ejemplo DNI.");
        }
        body.setDocumentType(documentType);
        String description = RecordValidation.requiredText(body.getDescription(), "Descripción de la medida", 4000);
        if (description.length() < 5) {
            throw invalidField("Descripción de la medida: ingresá al menos 5 caracteres.");
        }
        body.setDescription(description);
        body.setLegajoNumber(judicialReference(body.getLegajoNumber(), "Legajo judicial"));
        body.setMatrixNumber(judicialReference(body.getMatrixNumber(), "Matrícula"));
        body.setFolioNumber(judicialReference(body.getFolioNumber(), "Folio"));
        if (body.getInscriptionDate() != null && (body.getInscriptionDate().getYear() < 1000
                || body.getInscriptionDate().isAfter(LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))))) {
            throw invalidField("Fecha de inscripción: ingresá una fecha válida que no sea futura.");
        }
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

    private static String personName(String value, String label) {
        String name = RecordValidation.requiredText(value, label, 20);
        if (name.length() < 2 || !name.matches("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*")) {
            throw invalidField(label + ": ingresá entre 2 y 20 letras, con espacios, guiones o apóstrofes.");
        }
        return name;
    }

    private static String judicialReference(String value, String label) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.length() > 50 || !value.matches("[\\p{L}0-9]+(?:[ ./_-][\\p{L}0-9]+)*")) {
            throw invalidField(label + ": ingresá hasta 50 caracteres con letras, números y separadores.");
        }
        return value;
    }

    private static ResponseStatusException invalidField(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}