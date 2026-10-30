
package com.educore.sge.institution.web;

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
        public ClassScheduleController(ClassScheduleRepository schedules, ClassroomConfigRepository classrooms) {
            this.schedules = schedules;
            this.classrooms = classrooms;
        }
        @GetMapping
        public List<ClassScheduleJpaEntity> list(@RequestHeader("X-Institution-Id") String tenant) {
            return schedules.findByTenantId(tenant);
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

