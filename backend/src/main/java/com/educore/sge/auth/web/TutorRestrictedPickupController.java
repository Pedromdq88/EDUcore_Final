package com.educore.sge.auth.web;

import com.educore.sge.auth.RoleScopeService;
import com.educore.sge.institution.infrastructure.repository.JudicialRestrictionJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/my-students/{studentId}/restricted-pickups")
public class TutorRestrictedPickupController {
    private final StudentJpaRepository students;
    private final JudicialRestrictionJpaRepository restrictions;
    private final RoleScopeService scopes;

    public TutorRestrictedPickupController(StudentJpaRepository students,
                                          JudicialRestrictionJpaRepository restrictions,
                                          RoleScopeService scopes) {
        this.students = students;
        this.restrictions = restrictions;
        this.scopes = scopes;
    }

    public record RestrictedPickupView(String id, String firstName, String lastName,
                                       String documentType, String documentNumberMasked) {}

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping
    public List<RestrictedPickupView> list(@RequestHeader("X-Institution-Id") String tenantId,
                                           @PathVariable String studentId,
                                           Authentication authentication) {
        String tutorId = scopes.verifiedTutorId(authentication, tenantId);
        students.findById(studentId)
            .filter(student -> tenantId.equals(student.getTenantId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
        if (!students.existsByIdAndTutorsId(studentId, tutorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El alumno no está vinculado a tu cuenta");
        }
        return restrictions.findAllByTenantIdAndStudentId(tenantId, studentId).stream()
            .map(restriction -> new RestrictedPickupView(
                restriction.getId(), restriction.getFirstName(), restriction.getLastName(),
                restriction.getDocumentType(), maskDocument(restriction.getDocumentNumber())))
            .toList();
    }

    private static String maskDocument(String value) {
        if (value == null) return "No informado";
        String digits = value.replaceAll("\\D", "");
        return digits.length() >= 4 ? "••••" + digits.substring(digits.length() - 4) : "No informado";
    }
}
