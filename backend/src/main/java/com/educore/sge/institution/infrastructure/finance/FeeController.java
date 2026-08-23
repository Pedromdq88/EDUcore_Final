package com.educore.sge.institution.infrastructure.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/students/{studentId}/fees")
public class FeeController {

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TUTOR')")
    @GetMapping
    public ResponseEntity<List<FeeJpaEntity>> getStudentFees(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "2026") int academicYear) {
        return ResponseEntity.ok(feeService.getStudentFees(studentId, academicYear));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/toggle")
    public ResponseEntity<FeeJpaEntity> togglePayment(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "2026") int academicYear,
            @RequestParam int monthNumber,
            Principal principal) {
        String currentUserId = principal != null ? principal.getName() : "ADMIN";
        return ResponseEntity.ok(feeService.toggleFeePayment(studentId, academicYear, monthNumber, currentUserId));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PutMapping("/emails")
    public ResponseEntity<Void> updateEmails(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "2026") int academicYear,
            @RequestBody Map<String, String> body) {
        feeService.updateContactEmails(studentId, academicYear, body.get("receiptEmail"), body.get("queryEmail"));
        return ResponseEntity.ok().build();
    }
}