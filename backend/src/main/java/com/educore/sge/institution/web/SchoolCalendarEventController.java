package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.SchoolCalendarEventJpaEntity;
import com.educore.sge.institution.infrastructure.repository.SchoolCalendarEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar/events")
public class SchoolCalendarEventController {
    private final SchoolCalendarEventRepository repository;
    private final com.educore.sge.auth.RoleScopeService scopes;
    public SchoolCalendarEventController(SchoolCalendarEventRepository repository,com.educore.sge.auth.RoleScopeService scopes) { this.repository = repository; this.scopes=scopes; }
    @GetMapping
    public List<SchoolCalendarEventJpaEntity> list(@RequestHeader("X-Institution-Id") String tenant,
        org.springframework.security.core.Authentication authentication) {
        var events=repository.findByTenantId(tenant);
        if(scopes.is(authentication,"TUTOR"))return events.stream()
            .filter(e->"GENERAL".equals(e.getSection()) || "FAMILIAS".equals(e.getSection())).toList();
        return events;
    }
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    public SchoolCalendarEventJpaEntity create(@RequestHeader("X-Institution-Id") String tenant,
                                               @RequestBody SchoolCalendarEventJpaEntity event) {
        if (event.getTitle() == null || event.getTitle().isBlank() || event.getDate() == null
                || event.getCategory() == null || event.getSection() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento incompleto");
        event.setId(UUID.randomUUID().toString());
        event.setTenantId(tenant);
        return repository.save(event);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    public void delete(@RequestHeader("X-Institution-Id") String tenant, @PathVariable String id) {
        var event = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        if (!tenant.equals(event.getTenantId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Evento de otra institución");
        repository.delete(event);
    }
}