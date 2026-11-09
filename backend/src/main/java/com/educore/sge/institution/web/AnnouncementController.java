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
    private final com.educore.sge.auth.AuthUserRepository accounts;
    private final com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students;
    private final com.educore.sge.auth.RoleScopeService scopes;

    public AnnouncementController(AnnouncementJpaRepository repository,
        com.educore.sge.auth.AuthUserRepository accounts,
        com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students,
        com.educore.sge.auth.RoleScopeService scopes) {
        this.repository = repository;
        this.accounts = accounts;
        this.students=students;
        this.scopes=scopes;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR','TEACHER','TUTOR')")
    @GetMapping
    public List<AnnouncementJpaEntity> getAnnouncements(
            @RequestHeader("X-Institution-Id") String institutionId,
            org.springframework.security.core.Authentication authentication) {

        var notices=repository.findAllByTenantIdOrderByIsPinnedDescCreatedAtDesc(institutionId);
        if(scopes.is(authentication,"TUTOR")) {
            String tutorId=scopes.verifiedTutorId(authentication,institutionId);
            var linked=students.findByTenantIdAndTutor(institutionId,tutorId);
            var ids=linked.stream().map(s->s.getId()).collect(java.util.stream.Collectors.toSet());
            var rooms=linked.stream().map(s->s.getClassroom()).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            return notices.stream().filter(n-> "GLOBAL".equals(n.getScope()) ||
                ("CLASSROOM".equals(n.getScope()) && rooms.contains(n.getTargetClassroom())) ||
                ("PRIVATE_STUDENT".equals(n.getScope()) && ids.contains(n.getTargetStudentId()))).toList();
        }
        if(scopes.is(authentication,"TEACHER")){
            var rooms=scopes.teacherClassrooms(authentication,institutionId);
            var ids=students.findByTenantId(institutionId).stream().filter(s->rooms.contains(s.getClassroom()))
                .map(s->s.getId()).collect(java.util.stream.Collectors.toSet());
            return notices.stream().filter(n->"GLOBAL".equals(n.getScope()) ||
                ("CLASSROOM".equals(n.getScope()) && rooms.contains(n.getTargetClassroom())) ||
                ("PRIVATE_STUDENT".equals(n.getScope()) && ids.contains(n.getTargetStudentId()))).toList();
        }
        if(scopes.is(authentication,"PRECEPTOR")) return notices.stream().filter(n->!"PRIVATE_STUDENT".equals(n.getScope())).toList();
        return notices;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PostMapping
    public ResponseEntity<AnnouncementJpaEntity> createAnnouncement(
            @RequestHeader("X-Institution-Id") String institutionId,
            org.springframework.security.core.Authentication authentication,
            @RequestBody AnnouncementJpaEntity body) {

        if(scopes.is(authentication,"TEACHER")){
            var teacherRooms=scopes.teacherClassrooms(authentication,institutionId);
            boolean classroomNotice="CLASSROOM".equals(body.getScope())
                && teacherRooms.contains(body.getTargetClassroom());
            boolean observation="PRIVATE_STUDENT".equals(body.getScope())
                && "OBSERVACION".equals(body.getCategory())
                && body.getTargetStudentId()!=null
                && students.findById(body.getTargetStudentId()).stream().anyMatch(student ->
                   institutionId.equals(student.getTenantId()) && teacherRooms.contains(student.getClassroom()));
            if(!classroomNotice && !observation)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Los docentes solo pueden enviar avisos a sus aulas u observaciones a sus propios alumnos.");
            if(observation){body.setTargetClassroom(null);body.setIsPinned(false);}
        }
        if(scopes.is(authentication,"PRECEPTOR") && "PRIVATE_STUDENT".equals(body.getScope()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No podés publicar mensajes privados sin asignación.");
        if (body.getCreatedAt() == null) {
            body.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (body.getIsPinned() == null) {
            body.setIsPinned(false);
        }
        body.setId(UUID.randomUUID().toString());
        body.setTenantId(institutionId);
        body.setAuthorId(authentication.getName());
        body.setAuthorName(authentication.getName().split("@")[0]);
        body.setAuthorRole(authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_",""));
        return ResponseEntity.ok(repository.save(body));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementJpaEntity> updateAnnouncement(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            @RequestBody AnnouncementJpaEntity body,
            org.springframework.security.core.Authentication authentication) {

        return repository.findById(id).map(existing -> {
            if (!existing.getTenantId().equals(institutionId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No pertenece a esta institución.");
            }
            if(scopes.is(authentication,"TEACHER")){
                var rooms=scopes.teacherClassrooms(authentication,institutionId);
                if(!existing.getAuthorId().equalsIgnoreCase(authentication.getName()) ||
                    !"CLASSROOM".equals(existing.getScope()) || !rooms.contains(existing.getTargetClassroom()) ||
                    !"CLASSROOM".equals(body.getScope()) || !rooms.contains(body.getTargetClassroom()))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo podés editar tus avisos de aula.");
            }
            if(scopes.is(authentication,"PRECEPTOR") &&
                ("PRIVATE_STUDENT".equals(existing.getScope()) || "PRIVATE_STUDENT".equals(body.getScope())))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
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
            @PathVariable String id,
            org.springframework.security.core.Authentication authentication) {

        return repository.findById(id).map(announcement -> {
            if (!announcement.getTenantId().equals(institutionId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No pertenece a esta institución.");
            }
            if(scopes.is(authentication,"TEACHER") &&
                 (!announcement.getAuthorId().equalsIgnoreCase(authentication.getName()) ||
                  !scopes.teacherClassrooms(authentication,institutionId).contains(announcement.getTargetClassroom())))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo podés eliminar tus propios avisos de aula.");
            if(scopes.is(authentication,"PRECEPTOR") && "PRIVATE_STUDENT".equals(announcement.getScope()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            repository.delete(announcement);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}