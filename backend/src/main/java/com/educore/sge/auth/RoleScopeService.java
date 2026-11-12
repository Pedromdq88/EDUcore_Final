package com.educore.sge.auth;

import com.educore.sge.institution.infrastructure.repository.StaffJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import java.util.stream.Collectors;

/** Ámbitos de lectura calculados a partir de fichas institucionales, no del navegador. */
@Service
public class RoleScopeService {
    private final AuthUserRepository accounts;
    private final StaffJpaRepository staff;
    private final com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository tutors;
    public RoleScopeService(AuthUserRepository accounts, StaffJpaRepository staff,
                            com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository tutors) {
        this.accounts=accounts; this.staff=staff; this.tutors=tutors;
    }

    /**
     * El rol TUTOR por sí solo no habilita datos de alumnos. El legajo, correo
     * e institución deben coincidir con la cuenta que inició sesión.
     */
    public String verifiedTutorId(Authentication authentication, String tenantId) {
        if (!is(authentication, "TUTOR"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no tiene rol de tutor");
        var account = accounts.findByTenantIdAndEmailIgnoreCase(tenantId, authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no encontrada"));
        if (!account.enabled || account.tutorId == null || account.tutorId.isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La cuenta aún no está asociada a un legajo de tutor. Solicitá que la institución verifique el vínculo.");
        var tutor = tutors.findById(account.tutorId)
            .filter(t -> tenantId.equals(t.getTenantId()) && t.getEmail() != null
                && t.getEmail().trim().equalsIgnoreCase(account.email))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                "El legajo de tutor no coincide con la cuenta. Solicitá revisión a la institución."));
        return tutor.getId();
    }
    public boolean is(Authentication a, String role) {
        return a != null && a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_"+role));
    }
    public Set<String> teacherClassrooms(Authentication a, String tenant) {
        if (!is(a,"TEACHER")) return Set.of();
        var user=accounts.findByTenantIdAndEmailIgnoreCase(tenant,a.getName())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Cuenta sin institución asociada"));
        if (user.staffId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Docente sin ficha vinculada");
        var teacher=staff.findById(user.staffId).filter(s->tenant.equals(s.getTenantId()) && "ACTIVE".equalsIgnoreCase(s.getStatus()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Docente no habilitado"));
        Set<String> rooms=teacher.getAssignments().stream().map(x->x.getClassroom())
            .filter(x->x!=null && !x.isBlank()).collect(Collectors.toSet());
        if(teacher.getClassroom()!=null && !teacher.getClassroom().isBlank())rooms.add(teacher.getClassroom());
        return rooms;
    }
    public String teacherStaffId(Authentication a, String tenant) {
        var user=accounts.findByTenantIdAndEmailIgnoreCase(tenant,a.getName())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (user.staffId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Docente sin ficha vinculada");
        return user.staffId;
    }
}
