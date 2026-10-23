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
    public SchoolCalendarEventController(SchoolCalendarEventRepository repository) { this.repository = repository; }
    @GetMapping
    public List<SchoolCalendarEventJpaEntity> list(@RequestHeader("X-Institution-Id") String tenant) {
        return repository.findByTenantId(tenant);
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