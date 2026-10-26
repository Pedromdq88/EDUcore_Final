package com.educore.sge.auth;

import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/my-students")
public class TutorSelfController {
    private final AuthService auth;
    private final StudentJpaRepository students;
    public TutorSelfController(AuthService auth, StudentJpaRepository students) { this.auth = auth; this.students = students; }
    public record StudentSummary(String id, String firstName, String lastName, String classroom) {}
    @GetMapping @Transactional(readOnly = true)
    public List<StudentSummary> mine() {
        var current = auth.principal();
        if (!"TUTOR".equals(current.role()) || !"TUTOR".equals(current.personType()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo para tutores");
        return students.findByTenantId(current.tenantId()).stream()
            .filter(s -> s.getTutors() != null && s.getTutors().stream()
                .anyMatch(t -> current.personId().equals(t.getId()) && current.tenantId().equals(t.getTenantId())))
            .map(s -> new StudentSummary(s.getId(),s.getFirstName(),s.getLastName(),s.getClassroom()))
            .toList();
    }
}
