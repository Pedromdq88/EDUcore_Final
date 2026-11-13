package com.educore.sge.institution.web.academic;

import com.educore.sge.institution.infrastructure.domain.ClassroomShift;
import com.educore.sge.institution.infrastructure.entity.ClassroomConfigJpaEntity;
import com.educore.sge.institution.infrastructure.repository.ClassroomConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms-config")
public class ClassroomConfigController {

    // 1. Dejamos una sola inyección del repositorio
    @Autowired
    private ClassroomConfigRepository repository;
    @Autowired
    private com.educore.sge.auth.RoleScopeService scopes;
    @Autowired
    private com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository students;
    @Autowired
    private com.educore.sge.institution.infrastructure.repository.StaffJpaRepository staff;
    @Autowired
    private com.educore.sge.institution.infrastructure.repository.ClassScheduleRepository schedules;

    @GetMapping
    public List<ClassroomConfigJpaEntity> getClassrooms(
            @RequestHeader("X-Institution-Id") String institutionId,
            org.springframework.security.core.Authentication authentication) {
        var rooms=repository.findByTenantId(institutionId);
        if(scopes.is(authentication,"TEACHER")){
            var allowed=scopes.teacherClassrooms(authentication,institutionId);
            return rooms.stream().filter(room->allowed.contains(room.getName())).toList();
        }
        if (scopes.is(authentication, "TUTOR")) {
            var tutorId = scopes.verifiedTutorId(authentication, institutionId);
            var linked = students.findByTenantIdAndTutor(institutionId, tutorId);
            return rooms.stream().filter(room -> scopes.tutorCanReadClassroom(room, linked)).toList();
        }
        return rooms;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public List<ClassroomConfigJpaEntity> createClassroom(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody ClassroomConfigJpaEntity payload) {

        payload.setTenantId(institutionId);
        if (payload.getMinimumAge() == null) {
            payload.setMinimumAge(0);
        }
        if (payload.getShift() == null) {
            payload.setShift(ClassroomShift.MANANA);
        }

        List<ClassroomConfigJpaEntity> createdList = new java.util.ArrayList<>();
        String name=payload.getName()==null?"":payload.getName().trim();
        if(name.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nombre del aula obligatorio");
        if(payload.getShift()==ClassroomShift.AMBOS){
            // Dos registros físicos independientes para evitar conflictos de horarios.
            for(ClassroomShift turn : List.of(ClassroomShift.MANANA,ClassroomShift.TARDE)){
                String roomName=name+" ("+(turn==ClassroomShift.MANANA?"Mañana":"Tarde")+")";
                ensureAvailable(institutionId,roomName,turn,null);
                ClassroomConfigJpaEntity room=new ClassroomConfigJpaEntity();
                room.setId(UUID.randomUUID().toString());room.setTenantId(institutionId);
                room.setName(roomName);room.setMinimumAge(payload.getMinimumAge());room.setShift(turn);
                createdList.add(repository.save(room));
            }
        }else{
            ensureAvailable(institutionId,name,payload.getShift(),null);
            payload.setName(name);payload.setId(UUID.randomUUID().toString());
            createdList.add(repository.save(payload));
        }

        return createdList;
    }

    /** Cuatro categorías iniciales. No modifica aulas existentes ni las duplica. */
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/defaults")
    @org.springframework.transaction.annotation.Transactional
    public List<ClassroomConfigJpaEntity> createDefaults(@RequestHeader("X-Institution-Id") String institutionId) {
        if (!repository.findByTenantId(institutionId).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La institución ya tiene aulas: modificá las existentes desde el gestor.");
        java.util.List<ClassroomConfigJpaEntity> result = new java.util.ArrayList<>();
        String[] names = {"Maternal (1-2 años)", "Primera sección (3 años)", "Segunda sección (4 años)", "Tercera sección (5 años)"};
        int[] ages = {1,3,4,5};
        for (int i=0; i<names.length; i++) {
            for(ClassroomShift turn : List.of(ClassroomShift.MANANA,ClassroomShift.TARDE)){
                ClassroomConfigJpaEntity room=new ClassroomConfigJpaEntity();
                room.setId(UUID.randomUUID().toString());room.setTenantId(institutionId);
                room.setName(names[i]+" ("+(turn==ClassroomShift.MANANA?"Mañana":"Tarde")+")");
                room.setMinimumAge(ages[i]);room.setShift(turn);
                result.add(repository.save(room));
            }
        }
        return result;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public void deleteClassroom(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {
        ClassroomConfigJpaEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula no encontrada"));

        // 2. Agregamos validación de seguridad para evitar que otra escuela borre esta aula
        if (!entity.getTenantId().equals(institutionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar esta aula");
        }

        // Al eliminar un aula, limpiar las referencias por nombre para que los alumnos
        // aparezcan como "Sin asignar" y los docentes no conserven permisos de esa sala.
        for (var student : students.findByTenantId(institutionId)) {
            if (entity.getName().equals(student.getClassroom())) {
                student.setClassroom(null);
                students.save(student);
            }
        }
        for (var member : staff.findAllByTenantId(institutionId)) {
            boolean changed = false;
            if (entity.getName().equals(member.getClassroom())) {
                member.setClassroom(null);
                changed = true;
            }
            if (member.getAssignments().removeIf(a -> entity.getName().equals(a.getClassroom()))) {
                changed = true;
            }
            if (changed) staff.save(member);
        }
        // Evita horarios que apunten al ID de un aula inexistente.
        schedules.deleteAll(schedules.findByTenantId(institutionId).stream()
                .filter(s -> id.equals(s.getClassroomId())).toList());
        repository.delete(entity);
    }

    // 3. Agregamos protección de roles al PUT
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClassroom(
            @RequestHeader("X-Institution-Id") String tenantId,
            @PathVariable String id,
            @RequestBody ClassroomConfigJpaEntity payload) {
        try {
            Optional<ClassroomConfigJpaEntity> optionalClassroom = repository.findById(id);

            if (optionalClassroom.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El aula no existe");
            }

            ClassroomConfigJpaEntity existingClassroom = optionalClassroom.get();

            if (!existingClassroom.getTenantId().equals(tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para editar esta aula");
            }

            if(payload.getShift()==ClassroomShift.AMBOS && existingClassroom.getShift()!=ClassroomShift.AMBOS)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                   "El turno Ambos se elige al crear una nueva aula, para generar dos registros.");
            ensureAvailable(tenantId,payload.getName().trim(),payload.getShift()==null?existingClassroom.getShift():payload.getShift(),id);
            existingClassroom.setName(payload.getName().trim());
            existingClassroom.setMinimumAge(payload.getMinimumAge());
            if (payload.getShift() != null) existingClassroom.setShift(payload.getShift());

            ClassroomConfigJpaEntity updatedClassroom = repository.save(existingClassroom);

            return ResponseEntity.ok(updatedClassroom);

        } catch (ResponseStatusException e) { throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar el aula: " + e.getMessage());
        }
    }
    private void ensureAvailable(String tenant, String name, ClassroomShift shift, String ignoreId){
        if(repository.findByTenantId(tenant).stream().anyMatch(c->
            !c.getId().equals(ignoreId) && c.getName().equalsIgnoreCase(name) && c.getShift()==shift))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe esa aula con ese turno.");
    }

}