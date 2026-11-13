package com.educore.sge.kindergarten.web;

import com.educore.sge.auth.RoleScopeService;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StudentSiblingController {
    private final JdbcTemplate jdbc;
    private final StudentJpaRepository students;
    private final RoleScopeService scopes;

    public StudentSiblingController(JdbcTemplate jdbc, StudentJpaRepository students, RoleScopeService scopes) {
        this.jdbc = jdbc;
        this.students = students;
        this.scopes = scopes;
    }

    public record Sibling(String id, String firstName, String lastName, String classroom) {}
    public record SiblingOptions(List<Sibling> confirmed, List<Sibling> candidates) {}

    private StudentJpaEntity findStudent(String id, String tenantId) {
        return students.findById(id)
            .filter(student -> tenantId.equals(student.getTenantId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
    }

    private List<Sibling> confirmed(String studentId, String tenantId, String tutorId) {
        String query = """
            SELECT s.id, s.first_name, s.last_name, s.classroom
              FROM student_siblings links
              JOIN students s ON s.id = CASE WHEN links.student_id = ? THEN links.sibling_id ELSE links.student_id END
             WHERE links.tenant_id = ?
               AND (links.student_id = ? OR links.sibling_id = ?)
               AND s.tenant_id = ? AND s.status = 'ACTIVE'
            """;
        if (tutorId != null) query += " AND EXISTS (SELECT 1 FROM student_tutors st WHERE st.student_id = s.id AND st.tutor_id = ?)";
        query += " ORDER BY s.last_name, s.first_name";
        Object[] params = tutorId == null
            ? new Object[]{studentId, tenantId, studentId, studentId, tenantId}
            : new Object[]{studentId, tenantId, studentId, studentId, tenantId, tutorId};
        return jdbc.query(query, (rs, row) -> new Sibling(
            rs.getString("id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("classroom")), params);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR')")
    @GetMapping("/students/{studentId}/siblings")
    @Transactional(readOnly = true)
    public SiblingOptions options(@PathVariable String studentId,
                                  @RequestHeader("X-Institution-Id") String tenantId) {
        findStudent(studentId, tenantId);
        List<Sibling> candidates = jdbc.query("""
            SELECT DISTINCT other.id, other.first_name, other.last_name, other.classroom
              FROM student_tutors a
              JOIN student_tutors b ON b.tutor_id = a.tutor_id AND b.student_id <> a.student_id
              JOIN students other ON other.id = b.student_id
             WHERE a.student_id = ? AND other.tenant_id = ? AND other.status = 'ACTIVE'
               AND NOT EXISTS (
                   SELECT 1 FROM student_siblings links
                    WHERE links.tenant_id = ?
                      AND ((links.student_id = ? AND links.sibling_id = other.id)
                        OR (links.sibling_id = ? AND links.student_id = other.id)))
             ORDER BY other.last_name, other.first_name
            """, (rs, row) -> new Sibling(
                rs.getString("id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("classroom")),
            studentId, tenantId, tenantId, studentId, studentId);
        return new SiblingOptions(confirmed(studentId, tenantId, null), candidates);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
    @PostMapping("/students/{studentId}/siblings/{siblingId}")
    @Transactional
    public void confirm(@PathVariable String studentId, @PathVariable String siblingId,
                        @RequestHeader("X-Institution-Id") String tenantId) {
        StudentJpaEntity student = findStudent(studentId, tenantId);
        StudentJpaEntity sibling = findStudent(siblingId, tenantId);
        if (student.getId().equals(sibling.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un alumno no puede vincularse consigo mismo");
        }
        if (!"ACTIVE".equals(String.valueOf(student.getStatus())) || !"ACTIVE".equals(String.valueOf(sibling.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ambos alumnos deben estar activos");
        }
        Integer shared = jdbc.queryForObject("""
            SELECT COUNT(*) FROM student_tutors a
              JOIN student_tutors b ON a.tutor_id = b.tutor_id
             WHERE a.student_id = ? AND b.student_id = ?
            """, Integer.class, studentId, siblingId);
        if (shared == null || shared == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Los alumnos no comparten un tutor vinculado");
        }
        String first = studentId.compareTo(siblingId) < 0 ? studentId : siblingId;
        String second = studentId.compareTo(siblingId) < 0 ? siblingId : studentId;
        jdbc.update("""
            INSERT INTO student_siblings(tenant_id, student_id, sibling_id)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE confirmed_at = confirmed_at
            """, tenantId, first, second);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
    @DeleteMapping("/students/{studentId}/siblings/{siblingId}")
    @Transactional
    public void remove(@PathVariable String studentId, @PathVariable String siblingId,
                       @RequestHeader("X-Institution-Id") String tenantId) {
        findStudent(studentId, tenantId);
        findStudent(siblingId, tenantId);
        String first = studentId.compareTo(siblingId) < 0 ? studentId : siblingId;
        String second = studentId.compareTo(siblingId) < 0 ? siblingId : studentId;
        jdbc.update("DELETE FROM student_siblings WHERE tenant_id=? AND student_id=? AND sibling_id=?", tenantId, first, second);
    }

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/auth/my-students/{studentId}/siblings")
    @Transactional(readOnly = true)
    public List<Sibling> mySiblings(@PathVariable String studentId,
                                    @RequestHeader("X-Institution-Id") String tenantId,
                                    Authentication authentication) {
        String tutorId = scopes.verifiedTutorId(authentication, tenantId);
        findStudent(studentId, tenantId);
        if (!students.existsByIdAndTutorsId(studentId, tutorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Alumno no vinculado a tu cuenta");
        }
        return confirmed(studentId, tenantId, tutorId);
    }
}
