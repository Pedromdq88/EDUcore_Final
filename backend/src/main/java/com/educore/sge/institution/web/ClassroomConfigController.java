package com.educore.sge.institution.web;

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

    @GetMapping
    public List<ClassroomConfigJpaEntity> getClassrooms(
            @RequestHeader("X-Institution-Id") String institutionId) {
        return repository.findByTenantId(institutionId);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping
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

        if (ClassroomShift.AMBOS.equals(payload.getShift())) {
            ClassroomConfigJpaEntity manana = new ClassroomConfigJpaEntity();
            manana.setId(UUID.randomUUID().toString());
            manana.setTenantId(institutionId);
            manana.setName(payload.getName() + " (mañana)");
            manana.setMinimumAge(payload.getMinimumAge());
            manana.setShift(ClassroomShift.MANANA);
            createdList.add(repository.save(manana));

            ClassroomConfigJpaEntity tarde = new ClassroomConfigJpaEntity();
            tarde.setId(UUID.randomUUID().toString());
            tarde.setTenantId(institutionId);
            tarde.setName(payload.getName() + " (tarde)");
            tarde.setMinimumAge(payload.getMinimumAge());
            tarde.setShift(ClassroomShift.TARDE);
            createdList.add(repository.save(tarde));
        } else {
            payload.setId(UUID.randomUUID().toString());
            createdList.add(repository.save(payload));
        }

        return createdList;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @DeleteMapping("/{id}")
    public void deleteClassroom(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {
        ClassroomConfigJpaEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula no encontrada"));

        // 2. Agregamos validación de seguridad para evitar que otra escuela borre esta aula
        if (!entity.getTenantId().equals(institutionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar esta aula");
        }

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

            existingClassroom.setName(payload.getName());
            existingClassroom.setMinimumAge(payload.getMinimumAge());

            ClassroomConfigJpaEntity updatedClassroom = repository.save(existingClassroom);

            return ResponseEntity.ok(updatedClassroom);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar el aula: " + e.getMessage());
        }
    }
}