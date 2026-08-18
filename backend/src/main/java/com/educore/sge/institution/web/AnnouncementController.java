package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.AnnouncementJpaEntity;
import com.educore.sge.institution.infrastructure.repository.AnnouncementJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

    private final AnnouncementJpaRepository repository;

    public AnnouncementController(AnnouncementJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AnnouncementJpaEntity> getAnnouncements(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestHeader(value = "X-User-Role", defaultValue = "TUTOR") String userRole) {

        return repository.findAllByTenantIdOrderByIsPinnedDescCreatedAtDesc(institutionId);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PostMapping
    public ResponseEntity<AnnouncementJpaEntity> createAnnouncement(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody AnnouncementJpaEntity body) {

        if ("TEACHER".equals(userRole) && "GLOBAL".equals(body.getScope())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los docentes solo pueden emitir comunicados para su salita/curso asignado.");
        }

        body.setId(UUID.randomUUID().toString());
        body.setTenantId(institutionId);
        return ResponseEntity.ok(repository.save(body));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementJpaEntity> updateAnnouncement(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            @RequestBody AnnouncementJpaEntity body) {

        return repository.findById(id).map(existing -> {
            if (!existing.getTenantId().equals(institutionId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No pertenece a esta institución.");
            }
            existing.setTitle(body.getTitle());
            existing.setContent(body.getContent());
            existing.setCategory(body.getCategory());
            existing.setScope(body.getScope());
            existing.setTargetClassroom(body.getTargetClassroom());
            existing.setTargetStudentId(body.getTargetStudentId());
            existing.setMediaUrl(body.getMediaUrl());
            existing.setIsPinned(body.getIsPinned());
            return ResponseEntity.ok(repository.save(existing));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso no encontrado"));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {

        return repository.findById(id).map(announcement -> {
            if (!announcement.getTenantId().equals(institutionId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No pertenece a esta institución.");
            }
            repository.delete(announcement);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}