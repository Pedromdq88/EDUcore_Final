package com.educore.sge.institution.web.communications;

import com.educore.sge.institution.infrastructure.entity.AnnouncementJpaEntity;
import com.educore.sge.institution.infrastructure.repository.AnnouncementJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

    private final AnnouncementJpaRepository repository;
    private final com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students;
    private final com.educore.sge.auth.RoleScopeService scopes;
    private final org.springframework.context.ApplicationEventPublisher events;

    public AnnouncementController(AnnouncementJpaRepository repository,
        com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students,
        com.educore.sge.auth.RoleScopeService scopes,
        org.springframework.context.ApplicationEventPublisher events) {
        this.repository = repository;
        this.students=students;
        this.scopes=scopes;
        this.events=events;
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
            var rooms=linked.stream().map(s->s.getClassroom()).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
            return notices.stream().filter(n ->
                "GLOBAL".equals(n.getScope()) ||
                ("CLASSROOM".equals(n.getScope()) && rooms.contains(n.getTargetClassroom())) ||
                ("TEACHER_OBSERVATION".equals(n.getScope()) && ids.contains(n.getTargetStudentId())) ||
                ("PRIVATE_STUDENT".equals(n.getScope()) && ids.contains(n.getTargetStudentId()))).toList();
        }
        notices=notices.stream().filter(n -> !"PRIVATE_STUDENT".equals(n.getScope())).toList();
        if(scopes.is(authentication,"TEACHER")){
            var rooms=scopes.teacherClassrooms(authentication,institutionId);
            return notices.stream().filter(n->"GLOBAL".equals(n.getScope()) ||
                ("CLASSROOM".equals(n.getScope()) && rooms.contains(n.getTargetClassroom())) ||
                ("TEACHER_OBSERVATION".equals(n.getScope())
                    && authentication.getName().equalsIgnoreCase(n.getAuthorId()))).toList();
        }
        return notices;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PostMapping
    public ResponseEntity<AnnouncementJpaEntity> createAnnouncement(
            @RequestHeader("X-Institution-Id") String institutionId,
            org.springframework.security.core.Authentication authentication,
            @RequestBody AnnouncementJpaEntity body) {

        if (body == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresá el comunicado.");
        if ("PRIVATE_STUDENT".equals(body.getScope()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Los avisos privados se crean desde el perfil del alumno.");
        if (!"GLOBAL".equals(body.getScope()) && !"CLASSROOM".equals(body.getScope()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destinatario inválido.");
        if (body.getTitle() == null || body.getTitle().isBlank() || body.getContent() == null || body.getContent().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título y el contenido son obligatorios.");
        if(scopes.is(authentication,"TEACHER") &&
            !("CLASSROOM".equals(body.getScope()) && scopes.teacherClassrooms(authentication,institutionId).contains(body.getTargetClassroom())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo podés enviar avisos a tus aulas.");
        if (body.getCreatedAt() == null) {
            body.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (body.getIsPinned() == null) {
            body.setIsPinned(false);
        }
        body.setTargetStudentId(null);
        body.setId(UUID.randomUUID().toString());
        body.setTenantId(institutionId);
        body.setAuthorId(authentication.getName());
        body.setAuthorName(authentication.getName().split("@")[0]);
        body.setAuthorRole(authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_",""));
        return ResponseEntity.ok(repository.save(body));
    }

    public record PrivateNoticeRequest(String title, String content, String category) {}

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TUTOR')")
    @GetMapping("/students/{studentId}/private")
    public List<AnnouncementJpaEntity> getPrivateNotices(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            org.springframework.security.core.Authentication authentication) {
        students.findById(studentId)
            .filter(s->institutionId.equals(s.getTenantId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado."));
        if(scopes.is(authentication,"TUTOR")) {
            String tutorId=scopes.verifiedTutorId(authentication,institutionId);
            boolean linked=students.findByTenantIdAndTutor(institutionId,tutorId)
                .stream().anyMatch(s->studentId.equals(s.getId()));
            if(!linked)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No tenés acceso a este alumno.");
        }
        return repository.findAllByTenantIdOrderByIsPinnedDescCreatedAtDesc(institutionId)
            .stream().filter(n->"PRIVATE_STUDENT".equals(n.getScope())
                && studentId.equals(n.getTargetStudentId())).toList();
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR')")
    @PutMapping("/students/{studentId}/private/{id}")
    public ResponseEntity<AnnouncementJpaEntity> updatePrivateNotice(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            @PathVariable String id,
            @RequestBody PrivateNoticeRequest request) {
        students.findById(studentId)
            .filter(s->institutionId.equals(s.getTenantId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado."));
        if(request==null || request.title()==null || request.title().isBlank() || request.title().length()>200
                || request.content()==null || request.content().isBlank() || request.content().length()>10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completá título y mensaje.");
        if(request.category()==null || !java.util.Set.of("GENERAL","IMPORTANTE","URGENTE").contains(request.category()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Categoría inválida.");
        AnnouncementJpaEntity notice=repository.findById(id)
            .filter(n->institutionId.equals(n.getTenantId())
                && "PRIVATE_STUDENT".equals(n.getScope()) && studentId.equals(n.getTargetStudentId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Aviso privado no encontrado."));
        notice.setTitle(request.title().trim());
        notice.setContent(request.content().trim());
        notice.setCategory(request.category());
        return ResponseEntity.ok(repository.save(notice));
    }


    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR')")
    @PostMapping("/students/{studentId}/private")
    public ResponseEntity<AnnouncementJpaEntity> createPrivateNotice(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            org.springframework.security.core.Authentication authentication,
            @RequestBody PrivateNoticeRequest request) {

        var student=students.findById(studentId)
            .filter(s->institutionId.equals(s.getTenantId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Alumno no encontrado."));
        if(request == null || request.title()==null || request.title().isBlank() || request.title().length()>200 ||
                request.content()==null || request.content().isBlank() || request.content().length()>10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completá título y mensaje.");
        String category=request.category();
        if(category == null || !java.util.Set.of("GENERAL","IMPORTANTE","URGENTE").contains(category))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Categoría inválida.");
        AnnouncementJpaEntity notice=new AnnouncementJpaEntity();
        notice.setId(UUID.randomUUID().toString());
        notice.setTenantId(institutionId);
        notice.setAuthorId(authentication.getName());
        notice.setAuthorName(authentication.getName().split("@")[0]);
        notice.setAuthorRole(authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_",""));
        notice.setTitle(request.title().trim());
        notice.setContent(request.content().trim());
        notice.setCategory(category);
        notice.setScope("PRIVATE_STUDENT");
        notice.setTargetStudentId(studentId);
        notice.setTargetClassroom(null);
        notice.setMediaUrl(null);
        notice.setIsPinned(false);
        AnnouncementJpaEntity saved=repository.save(notice);

        students.findPrimaryTutorId(studentId).ifPresent(tutorId -> student.getTutors().stream()
            .filter(t->tutorId.equals(t.getId()))
            .map(com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity::getEmail)
            .filter(mail->mail!=null && mail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            .findFirst()
            .ifPresent(mail->events.publishEvent(new com.educore.sge.auth.mail.EmailDispatchRequested(
                mail,"EDUcore - Tenés un aviso privado",
                "Tenés un nuevo aviso privado en EDUcore.\nIngresá con tu cuenta para leerlo.\n" +
                "Si no reconocés este aviso, contactá a la institución."))));

        return ResponseEntity.ok(saved);
    }

    public record TeacherObservationRequest(String content) {}

    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/students/{studentId}/observations")
    public ResponseEntity<AnnouncementJpaEntity> createTeacherObservation(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId,
            org.springframework.security.core.Authentication authentication,
            @RequestBody TeacherObservationRequest request) {

        var student=students.findById(studentId)
            .filter(s->institutionId.equals(s.getTenantId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Alumno no encontrado."));
        if(student.getClassroom()==null ||
                !scopes.teacherClassrooms(authentication,institutionId).contains(student.getClassroom()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No tenés acceso a este alumno.");
        if(request == null || request.content()==null || request.content().isBlank() || request.content().length()>10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completá la observación.");

        AnnouncementJpaEntity notice=new AnnouncementJpaEntity();
        notice.setId(UUID.randomUUID().toString());
        notice.setTenantId(institutionId);
        notice.setAuthorId(authentication.getName());
        notice.setAuthorName(authentication.getName().split("@")[0]);
        notice.setAuthorRole("TEACHER");
        notice.setTitle("Observación de " + student.getFirstName());
        notice.setContent(request.content().trim());
        notice.setCategory("OBSERVACION");
        notice.setScope("TEACHER_OBSERVATION");
        notice.setTargetStudentId(studentId);
        notice.setTargetClassroom(null);
        notice.setMediaUrl(null);
        notice.setIsPinned(false);
        return ResponseEntity.ok(repository.save(notice));
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
            if(java.util.Set.of("PRIVATE_STUDENT", "TEACHER_OBSERVATION").contains(existing.getScope()) ||
                java.util.Set.of("PRIVATE_STUDENT", "TEACHER_OBSERVATION").contains(body.getScope()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Los avisos privados se gestionan desde el perfil del alumno.");
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

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR')")
    @DeleteMapping("/students/{studentId}/private/{id}")
    public ResponseEntity<Void> deletePrivateNotice(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String studentId, @PathVariable String id) {
        AnnouncementJpaEntity notice=repository.findById(id)
            .filter(n->institutionId.equals(n.getTenantId()) &&
                    "PRIVATE_STUDENT".equals(n.getScope()) && studentId.equals(n.getTargetStudentId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Aviso privado no encontrado."));
        repository.delete(notice);
        return ResponseEntity.noContent().build();
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
            if(java.util.Set.of("PRIVATE_STUDENT", "TEACHER_OBSERVATION").contains(announcement.getScope()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Este aviso se gestiona desde el perfil del alumno.");
            if(scopes.is(authentication,"TEACHER") &&
                 (!announcement.getAuthorId().equalsIgnoreCase(authentication.getName()) ||
                  (!"PRIVATE_STUDENT".equals(announcement.getScope()) &&
                   !scopes.teacherClassrooms(authentication,institutionId).contains(announcement.getTargetClassroom()))))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo podés eliminar tus propios avisos de aula.");
            repository.delete(announcement);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}