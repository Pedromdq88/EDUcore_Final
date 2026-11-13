
package com.educore.sge.institution.web.academic;

import com.educore.sge.institution.infrastructure.entity.ClassScheduleJpaEntity;
import com.educore.sge.institution.infrastructure.repository.ClassScheduleRepository;
import com.educore.sge.institution.infrastructure.repository.ClassroomConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

    @RestController
    @RequestMapping("/api/v1/class-schedules")
    public class ClassScheduleController {
        private final ClassScheduleRepository schedules;
        private final ClassroomConfigRepository classrooms;
        private final com.educore.sge.auth.RoleScopeService scopes;
        private final com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students;
        public ClassScheduleController(ClassScheduleRepository schedules, ClassroomConfigRepository classrooms, com.educore.sge.auth.RoleScopeService scopes, com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students) {
            this.schedules = schedules;
            this.classrooms = classrooms;
            this.scopes=scopes;
            this.students = students;
        }
        @GetMapping
        public List<ClassScheduleJpaEntity> list(@RequestHeader("X-Institution-Id") String tenant,
            org.springframework.security.core.Authentication authentication) {
            var entries=schedules.findByTenantId(tenant);
            if(scopes.is(authentication,"TEACHER")){
                var allowed=scopes.teacherClassrooms(authentication,tenant);
                var ids=classrooms.findByTenantId(tenant).stream().filter(c->allowed.contains(c.getName()))
                    .map(c->c.getId()).collect(java.util.stream.Collectors.toSet());
                return entries.stream().filter(e->ids.contains(e.getClassroomId())).toList();
            }
            if (scopes.is(authentication, "TUTOR")) {
                var tutorId = scopes.verifiedTutorId(authentication, tenant);
                var linked = students.findByTenantIdAndTutor(tenant, tutorId);
                var ids = classrooms.findByTenantId(tenant).stream()
                    .filter(room -> scopes.tutorCanReadClassroom(room, linked))
                    .map(room -> room.getId()).collect(java.util.stream.Collectors.toSet());
                return entries.stream().filter(entry -> ids.contains(entry.getClassroomId())).toList();
            }
            return entries;
        }
        @PostMapping
        @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
        public ClassScheduleJpaEntity create(@RequestHeader("X-Institution-Id") String tenant,
                                             @RequestBody ClassScheduleJpaEntity item) {
            validate(tenant, item, null);
            item.setId(UUID.randomUUID().toString());
            item.setTenantId(tenant);
            return schedules.save(item);
        }
        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
        public ClassScheduleJpaEntity update(@RequestHeader("X-Institution-Id") String tenant,
                                             @PathVariable String id, @RequestBody ClassScheduleJpaEntity item) {
            ClassScheduleJpaEntity old = owned(tenant, id);
            validate(tenant, item, id);
            old.setClassroomId(item.getClassroomId());
            old.setSubject(item.getSubject().trim());
            old.setTeacherName(item.getTeacherName().trim());
            old.setDayOfWeek(item.getDayOfWeek());
            old.setStartTime(item.getStartTime());
            old.setEndTime(item.getEndTime());
            return schedules.save(old);
        }
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
        public void delete(@RequestHeader("X-Institution-Id") String tenant, @PathVariable String id) {
            schedules.delete(owned(tenant, id));
        }
        private ClassScheduleJpaEntity owned(String tenant, String id) {
            ClassScheduleJpaEntity item = schedules.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horario no encontrado"));
            if (!tenant.equals(item.getTenantId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Horario de otra institución");
            return item;
        }
        private void validate(String tenant, ClassScheduleJpaEntity item, String ignoreId) {
            if (item.getClassroomId() == null || item.getSubject() == null || item.getSubject().isBlank()
                    || item.getTeacherName() == null || item.getTeacherName().isBlank()
                    || item.getDayOfWeek() == null || item.getDayOfWeek() < 1 || item.getDayOfWeek() > 7
                    || item.getStartTime() == null || item.getEndTime() == null
                    || !item.getStartTime().isBefore(item.getEndTime()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de horario inválidos");
            var classroom = classrooms.findById(item.getClassroomId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aula inexistente"));
            if (!tenant.equals(classroom.getTenantId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aula de otra institución");
            for (var existing : schedules.findByTenantId(tenant)) {
                if (existing.getId().equals(ignoreId) || !existing.getDayOfWeek().equals(item.getDayOfWeek())) continue;
                boolean overlapping = item.getStartTime().isBefore(existing.getEndTime())
                        && existing.getStartTime().isBefore(item.getEndTime());
                if (overlapping && (existing.getClassroomId().equals(item.getClassroomId())
                        || existing.getTeacherName().trim().equalsIgnoreCase(item.getTeacherName().trim())))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Superposición de aula o docente");
            }
        }
    }

