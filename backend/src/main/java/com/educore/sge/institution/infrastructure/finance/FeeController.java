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
    private final com.educore.sge.auth.AuthService auth;
    private final com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students;
    private final com.educore.sge.auth.RoleScopeService scopes;

    public FeeController(FeeService feeService, com.educore.sge.auth.AuthService auth,
            com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students,
            com.educore.sge.auth.RoleScopeService scopes) {
        this.feeService = feeService; this.auth = auth; this.students = students; this.scopes = scopes;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TUTOR')")
    @GetMapping
    public ResponseEntity<List<FeeJpaEntity>> getStudentFees(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @RequestParam(defaultValue = "2026") int academicYear,
            @RequestHeader(value="Authorization",required=false) String authorization,
            org.springframework.security.core.Authentication authentication) {
        boolean tutor=false;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            var user = auth.authenticate(authorization.substring(7), institutionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
            tutor = user.role == com.educore.sge.shared.Rol.TUTOR;
            if (tutor &&
                !students.existsByIdAndTutorsId(studentId,scopes.verifiedTutorId(authentication,institutionId)))
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Alumno no vinculado a la cuenta.");
        }
        return ResponseEntity.ok(tutor ? feeService.peekStudentFees(institutionId, studentId, academicYear) : feeService.getStudentFees(institutionId, studentId, academicYear));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/toggle")
    public ResponseEntity<FeeJpaEntity> togglePayment(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @RequestParam(defaultValue = "2026") int academicYear,
            @RequestParam int monthNumber,
            Principal principal) {
        String currentUserId = principal != null ? principal.getName() : "ADMIN";
        return ResponseEntity.ok(feeService.toggleFeePayment(institutionId, studentId, academicYear, monthNumber, currentUserId));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PutMapping("/emails")
    public ResponseEntity<Void> updateEmails(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @RequestParam(defaultValue = "2026") int academicYear,
            @RequestBody Map<String, String> body) {
        feeService.updateContactEmails(institutionId, studentId, academicYear, body.get("receiptEmail"), body.get("queryEmail"));
        return ResponseEntity.ok().build();
    }
}