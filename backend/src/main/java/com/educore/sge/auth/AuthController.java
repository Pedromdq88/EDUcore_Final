package com.educore.sge.auth;

import com.educore.sge.auth.security.LoginAttemptLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final LoginAttemptLimiter loginLimiter;
    private final TutorJpaRepository tutors;
    private final AuthUserRepository users;
    private final StudentJpaRepository students;
    private final RoleScopeService scopes;
    private final com.educore.sge.institution.infrastructure.repository.StaffJpaRepository staff;
    public AuthController(AuthService auth, TutorJpaRepository tutors, StudentJpaRepository students,
         AuthUserRepository users, com.educore.sge.institution.infrastructure.repository.StaffJpaRepository staff,
         RoleScopeService scopes, LoginAttemptLimiter loginLimiter) {
        this.auth = auth; this.tutors = tutors; this.students = students; this.users = users; this.staff=staff;
        this.scopes = scopes;
        this.loginLimiter = loginLimiter;
    }
    public record LoginRequest(String institutionId, String email, String password) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    @PostMapping("/login")
    public AuthService.LoginResult login(@RequestBody LoginRequest body, HttpServletRequest request) {
        if (body == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de acceso incompletos.");
        String ip = request.getRemoteAddr(); // No confiar en X-Forwarded-For no validado.
        loginLimiter.check(body.institutionId(), body.email(), ip);
        try {
            AuthService.LoginResult result = auth.login(body.institutionId(), body.email(), body.password());
            loginLimiter.succeeded(body.institutionId(), body.email(), ip);
            return result;
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 401)
                loginLimiter.failed(body.institutionId(), body.email(), ip);
            throw ex;
        }
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication,
                                  @RequestHeader("X-Institution-Id") String institutionId,
                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authentication == null || !authentication.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        boolean mustChange = authorization != null && authorization.startsWith("Bearer ") &&
            auth.authenticate(authorization.substring(7), institutionId)
                .map(user -> user.mustChangePassword).orElse(false);
        return Map.of("email", authentication.getName(), "role", authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""), "institutionId", institutionId,
            "requiresPasswordChange", mustChange);
    }

    /** Devuelve solo hijos de la cuenta TUTOR autenticada y de la institución activa. */
    public record MyStudent(String id, String firstName, String lastName, String classroom, Integer academicYear) {}
    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/my-students")
    public List<MyStudent> myStudents(Authentication principal,
                                     @RequestHeader("X-Institution-Id") String tenantId,
                                     @RequestHeader("Authorization") String bearer) {
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var user = auth.authenticate(bearer.substring(7), tenantId)
            .filter(account -> account.role == com.educore.sge.shared.Rol.TUTOR)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        // Nunca se obtiene la identidad del tutor solo del rol o de un id suelto.
        String tutorId = scopes.verifiedTutorId(principal, tenantId);
        return students.findByTenantIdAndTutor(tenantId, tutorId).stream()
            .map(s -> new MyStudent(s.getId(), s.getFirstName(), s.getLastName(), s.getClassroom(), s.getAcademicYear())).toList();
    }

    /** Perfil escolar del hijo: se valida el vínculo con el Tutor en el servidor. */
    public record LinkedTutor(String id, String firstName, String lastName, String relationship, String phone) {}
    public record MyStudentDetail(String id, String firstName, String lastName, String documentNumber,
            String legajoNumber, java.time.LocalDate birthDate, String classroom, Integer academicYear,
            String shift, String status, String nationality, String address, String contactPhone,
            String cuil, String genderIdentity, String birthCountry,
            String birthProvince, String birthLocality, String addressDistrict, Boolean hasSiblings,
            Integer siblingCount, String primaryTutorId, List<LinkedTutor> tutors) {}

    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/my-students/{studentId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public MyStudentDetail myStudentDetail(org.springframework.security.core.Authentication authentication,
            @RequestHeader("X-Institution-Id") String tenantId, @PathVariable String studentId) {
        String tutorId = scopes.verifiedTutorId(authentication, tenantId);
        var student = students.findById(studentId)
            .filter(s -> tenantId.equals(s.getTenantId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
        if (!students.existsByIdAndTutorsId(studentId, tutorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este alumno no está vinculado a tu cuenta de tutor");
        }
        var family = student.getTutors() == null ? List.<LinkedTutor>of() : student.getTutors().stream()
            .map(t -> new LinkedTutor(t.getId(), t.getFirstName(), t.getLastName(),
                t.getRelationship() == null ? "" : t.getRelationship().name(), t.getPhone()))
            .toList();
        String address = student.getAddress();
        if (address == null || address.isBlank()) {
            address = String.join(" ", java.util.stream.Stream.of(student.getStreet(), student.getStreetNumber(),
                    student.getAddressLocality()).filter(v -> v != null && !v.isBlank()).toList());
        }
        String primaryTutorId = students.findPrimaryTutorId(studentId).orElse("");
        String contact = primaryTutorId.isBlank() ? null : tutors.findById(primaryTutorId)
            .filter(t -> tenantId.equals(t.getTenantId()))
            .map(com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity::getPhone)
            .orElse(null);
        return new MyStudentDetail(student.getId(), student.getFirstName(), student.getLastName(),
            student.getDocumentNumber(), student.getLegajoNumber(), student.getBirthDate(),
            student.getClassroom(), student.getAcademicYear(),
            student.getStudentShift() == null ? "" : student.getStudentShift().name(),
            student.getStatus() == null ? "" : student.getStatus().name(),
            student.getNationality(), address, contact, student.getCuil(),
            student.getGenderIdentity() == null ? "" : student.getGenderIdentity().name(),
            student.getBirthCountry() == null ? "" : student.getBirthCountry().name(),
            student.getBirthProvince(), student.getBirthLocality(), student.getAddressDistrict(),
            student.getHasSiblings(), student.getSiblingCount(), primaryTutorId, family);
    }

    public record MyTutorProfile(String id, String firstName, String lastName, String email) {}
    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/my-tutor")
    public MyTutorProfile myTutor(Authentication authentication,
            @RequestHeader("X-Institution-Id") String tenantId) {
        String tutorId = scopes.verifiedTutorId(authentication, tenantId);
        var tutor = tutors.findById(tutorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Legajo de tutor no encontrado"));
        return new MyTutorProfile(tutor.getId(), tutor.getFirstName(), tutor.getLastName(), tutor.getEmail());
    }

    public record MyAssignment(String classroom, String subject, String shift) {}
    public record MyStaffProfile(String id, String firstName, String lastName, String email,
        String role, String phone, java.util.List<MyAssignment> assignments) {}
    @PreAuthorize("hasAnyRole('TEACHER','PRECEPTOR')")
    @GetMapping("/my-staff")
    public MyStaffProfile myStaff(Authentication authentication,
        @RequestHeader("X-Institution-Id") String tenantId) {
        var account=users.findByTenantIdAndEmailIgnoreCase(tenantId, authentication.getName())
            .filter(u->u.staffId!=null)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Cuenta sin legajo de personal"));
        var person=staff.findById(account.staffId)
            .filter(s->tenantId.equals(s.getTenantId()) && "ACTIVE".equalsIgnoreCase(s.getStatus()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Legajo no habilitado"));
        return new MyStaffProfile(person.getId(),person.getFirstName(),person.getLastName(),
            person.getEmail(),person.getRole().name(),person.getPhone(),
            person.getAssignments().stream().map(a->new MyAssignment(a.getClassroom(),a.getSubject(),
                a.getShift()==null?"":a.getShift().name())).toList());
    }

    public record AuthorizedUser(String id, String email, String role, boolean enabled, String tutorId, String staffId, boolean mustChangePassword) {}
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
    @GetMapping("/authorized-users")
    public List<AuthorizedUser> authorizedUsers(@RequestHeader("X-Institution-Id") String tenantId) {
        return users.findAll().stream().filter(u -> tenantId.equals(u.tenantId))
            .map(u -> new AuthorizedUser(u.id,u.email,u.role.name(),u.enabled,u.tutorId,u.staffId,u.mustChangePassword))
            .sorted(java.util.Comparator.comparing(AuthorizedUser::email)).toList();
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value="Authorization", required=false) String bearer) {
        if (bearer != null && bearer.startsWith("Bearer ")) auth.logout(bearer.substring(7));
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@RequestHeader("X-Institution-Id") String tenant,
                                               @RequestHeader("Authorization") String bearer,
                                               Authentication authentication,
                                               @RequestBody ChangePasswordRequest body) {
        auth.changePassword(tenant, authentication.getName(), body.currentPassword(), body.newPassword(), bearer.substring(7));
        return Map.of("message", "Contraseña cambiada. Iniciá sesión nuevamente.");
    }
}
