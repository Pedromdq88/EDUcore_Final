package com.educore.sge.auth;

import com.educore.sge.institution.infrastructure.entity.StaffJpaEntity;
import com.educore.sge.institution.infrastructure.repository.StaffJpaRepository;
import com.educore.sge.shared.validation.RecordValidation;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/profile/staff")
@PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR','TEACHER')")
public class MyStaffProfileController {
    private final AuthUserRepository users;
    private final StaffJpaRepository staff;

    public MyStaffProfileController(AuthUserRepository users, StaffJpaRepository staff) {
        this.users = users;
        this.staff = staff;
    }

    public record Assignment(String classroom, String subject, String shift) {}
    public record StaffProfile(String id, String firstName, String lastName,
            String documentNumber, String phone, String email, String role,
            LocalDate hireDate, String classroom, List<Assignment> assignments) {}
    public record PersonalUpdate(String firstName, String lastName,
            String documentNumber, String phone) {}

    private StaffJpaEntity currentStaff(Authentication authentication, String tenantId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var account = users.findByTenantIdAndEmailIgnoreCase(tenantId, authentication.getName())
            .filter(u -> u.enabled && u.staffId != null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta sin legajo de personal"));
        return staff.findById(account.staffId)
            .filter(p -> tenantId.equals(p.getTenantId()) && "ACTIVE".equalsIgnoreCase(p.getStatus()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Legajo no habilitado"));
    }

    private StaffProfile toProfile(StaffJpaEntity person) {
        var assignments = person.getAssignments().stream()
            .map(a -> new Assignment(a.getClassroom(), a.getSubject(),
                a.getShift() == null ? "" : a.getShift().name())).toList();
        return new StaffProfile(person.getId(), person.getFirstName(), person.getLastName(),
            person.getDocumentNumber(), person.getPhone(), person.getEmail(),
            person.getRole().name(), person.getHireDate(), person.getClassroom(), assignments);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public StaffProfile get(Authentication authentication,
            @RequestHeader("X-Institution-Id") String tenantId) {
        return toProfile(currentStaff(authentication, tenantId));
    }

    @PutMapping
    @Transactional
    public StaffProfile update(Authentication authentication,
            @RequestHeader("X-Institution-Id") String tenantId,
            @RequestBody PersonalUpdate data) {
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos personales incompletos");
        }
        var person = currentStaff(authentication, tenantId);
        String firstName = RecordValidation.requiredText(data.firstName(), "Nombre", 20);
        String lastName = RecordValidation.requiredText(data.lastName(), "Apellido", 20);
        if (firstName.length() < 2 || lastName.length() < 2 ||
                !firstName.matches("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$") ||
                !lastName.matches("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre y apellido deben tener de 2 a 20 caracteres, sin números");
        }
        String phone = RecordValidation.optionalPhone(data.phone(), "personal");
        String dni = data.documentNumber() == null ? "" : data.documentNumber().trim();
        if (!dni.isEmpty()) {
            if (!dni.matches("[0-9]{6,8}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El DNI debe contener entre 6 y 8 números, sin puntos ni letras.");
            }
            for (var other : staff.findAllByTenantId(tenantId)) {
                if (!person.getId().equals(other.getId()) &&
                    dni.equals(RecordValidation.comparableDni(other.getDocumentNumber()))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe personal con ese DNI.");
                }
            }
        }
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setPhone(phone);
        person.setDocumentNumber(dni.isEmpty() ? null : dni);
        return toProfile(staff.save(person));
    }
}
