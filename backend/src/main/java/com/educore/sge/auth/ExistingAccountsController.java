package com.educore.sge.auth;

import com.educore.sge.institution.infrastructure.repository.StaffJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class ExistingAccountsController {
    private final AuthService auth;
    private final StaffJpaRepository staff;
    private final TutorJpaRepository tutors;
    public ExistingAccountsController(AuthService auth, StaffJpaRepository staff, TutorJpaRepository tutors) {
        this.auth = auth; this.staff = staff; this.tutors = tutors;
    }
    public record InviteRequest(String personType, String personId) {}
    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
    @PostMapping("/provision-existing")
    public void sendAccess(@RequestHeader("X-Institution-Id") String tenantId,
                            @RequestBody InviteRequest request) {
        if (request.personId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta ID");
        if ("STAFF".equals(request.personType())) {
            var p=staff.findById(request.personId())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Personal inexistente"));
            if (!tenantId.equals(p.getTenantId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            auth.provision(tenantId,"STAFF",p.getId(),p.getEmail(),p.getRole().name());
        } else if ("TUTOR".equals(request.personType())) {
            var p=tutors.findById(request.personId())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Tutor inexistente"));
            if (!tenantId.equals(p.getTenantId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            auth.provision(tenantId,"TUTOR",p.getId(),p.getEmail(),"TUTOR");
        } else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo válido: STAFF o TUTOR");
    }
}
