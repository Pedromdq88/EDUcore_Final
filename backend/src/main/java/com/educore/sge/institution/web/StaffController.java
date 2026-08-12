package com.educore.sge.institution.web;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.educore.sge.institution.infrastructure.entity.UserJpaEntity;
import com.educore.sge.institution.infrastructure.repository.UserJpaRepository;
import com.educore.sge.institution.persistence.StaffHistoryJpaEntity;
import com.educore.sge.institution.persistence.StaffHistoryRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID; // 🟢 Añadimos el import explícito para estar seguros

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/institution/staff")
public class StaffController {

    private final UserJpaRepository userRepository;

    @Autowired
    private StaffHistoryRepository staffHistoryRepository;

    public StaffController(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    // LISTAR PERSONAL ACTIVO
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @GetMapping
    public List<UserJpaEntity> getAllStaff() {
        return userRepository.findAll();
    }

    // CREAR NUEVO MIEMBRO DEL PERSONAL
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')") // 🟢 Corregido a hasAnyRole por las dudas
    @PostMapping
    public UserJpaEntity createStaff(@RequestBody UserJpaEntity staff) {
        // 🟢 Generamos el UUID usando la clase nativa de java
        staff.setId(UUID.randomUUID().toString());

        // 🟢 Corregido: Usamos 'userRepository' que es tu variable real del constructor
        return userRepository.save(staff);
    }

    // EDITAR FICHA DE PERSONAL
    @PreAuthorize("hasRole('DIRECTOR')")
    @PutMapping("/{id}")
    public UserJpaEntity updateStaff(@PathVariable String id, @RequestBody UserJpaEntity updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setFirstName(updatedUser.getFirstName());
            user.setLastName(updatedUser.getLastName());
            user.setEmail(updatedUser.getEmail());
            user.setRole(updatedUser.getRole());
            user.setClassroom(updatedUser.getClassroom());
            return userRepository.save(user);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    // PROCESAR BAJA HISTÓRICA CON VALIDACIÓN DE RANGOS
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{id}/baja")
    public void darDeBajaPersonal(@PathVariable String id) {
        UserJpaEntity user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Seguridad: Un administrativo no puede dar de baja a una Directora o al Owner
        boolean isAdministrative = SecurityContextHolder.getContext().getAuthentication()
            .getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATIVE"));

        if (isAdministrative && (user.getRole().name().equals("DIRECTOR") || user.getRole().name().equals("OWNER"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Seguridad: Un administrativo no puede dar de baja a un usuario de rango DIRECTOR u OWNER.");
        }

        // Mapeo a la tabla histórica
        StaffHistoryJpaEntity historico = new StaffHistoryJpaEntity(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole().name(),
            user.getClassroom(),
            user.getHireDate(),
            LocalDate.now()
        );
        staffHistoryRepository.save(historico);

        // Eliminación del registro activo
        userRepository.delete(user);
    }
}
