package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.StaffAssignmentJpaEntity;
import com.educore.sge.institution.infrastructure.entity.StaffJpaEntity;
import com.educore.sge.institution.infrastructure.repository.PortfolioJpaRepository;
import com.educore.sge.institution.infrastructure.repository.StaffJpaRepository;
import com.educore.sge.institution.persistence.StaffHistoryJpaEntity;
import com.educore.sge.institution.persistence.StaffHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping("/api/v1/institution/staff")
public class StaffController {

    private final StaffJpaRepository staffRepository;
    private final StaffHistoryRepository staffHistoryRepository;
    private final PortfolioJpaRepository portfolioRepository;

    public StaffController(StaffJpaRepository staffRepository,
                           StaffHistoryRepository staffHistoryRepository,
                           PortfolioJpaRepository portfolioRepository) {
        this.staffRepository = staffRepository;
        this.staffHistoryRepository = staffHistoryRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping
    public List<StaffJpaEntity> getAllStaff(
            @RequestHeader("X-Institution-Id") String institutionId) {
        return staffRepository.findAllByTenantId(institutionId);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping("/{id}")
    public StaffJpaEntity getStaffById(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {
        StaffJpaEntity staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal no encontrado"));

        if (!institutionId.equals(staff.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre personal de otra institución.");
        }
        return staff;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping
    public StaffJpaEntity createStaff(
            @RequestHeader(value = "X-Institution-Id") String institutionId,
            @RequestBody StaffJpaEntity staff) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Operación rechazada: No se especificó la institución escolar (X-Institution-Id)");
        }

        if (staff.getId() == null || staff.getId().isBlank()) {
            staff.setId(UUID.randomUUID().toString());
        }

        staff.setTenantId(institutionId);

        if (staff.getStatus() == null || staff.getStatus().isBlank()) {
            staff.setStatus("ACTIVE");
        }
        if (staff.getPassword() == null || staff.getPassword().isBlank()) {
            staff.setPassword("123");
        }

        if (staff.getAssignments() != null) {
            for (StaffAssignmentJpaEntity asg : staff.getAssignments()) {
                if (asg.getId() == null || asg.getId().isBlank()) {
                    asg.setId(UUID.randomUUID().toString());
                }
                asg.setTenantId(institutionId);
                asg.setStaff(staff);
            }
        }

        return staffRepository.save(staff);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PutMapping("/{id}")
    public StaffJpaEntity updateStaff(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            @RequestBody StaffJpaEntity updated) {

        StaffJpaEntity existing = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal no encontrado"));

        if (!institutionId.equals(existing.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre personal de otra institución.");
        }

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setRole(updated.getRole());
        existing.setClassroom(updated.getClassroom());

        if (updated.getDocumentNumber() != null) existing.setDocumentNumber(updated.getDocumentNumber());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        if (updated.getHireDate() != null) existing.setHireDate(updated.getHireDate());

        existing.getAssignments().clear();
        if (updated.getAssignments() != null && !updated.getAssignments().isEmpty()) {
            for (StaffAssignmentJpaEntity asg : updated.getAssignments()) {
                StaffAssignmentJpaEntity nuevaAsg = new StaffAssignmentJpaEntity();
                nuevaAsg.setId(asg.getId() != null && !asg.getId().isBlank() ? asg.getId() : UUID.randomUUID().toString());
                nuevaAsg.setSubject(asg.getSubject());
                nuevaAsg.setClassroom(asg.getClassroom());
                nuevaAsg.setShift(asg.getShift());
                nuevaAsg.setIsClassroomTeacher(asg.getIsClassroomTeacher() != null ? asg.getIsClassroomTeacher() : false);
                nuevaAsg.setTenantId(institutionId);
                nuevaAsg.setStaff(existing);
                existing.getAssignments().add(nuevaAsg);
            }
        }

        return staffRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{id}/baja")
    public void darDeBajaStaff(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {

        StaffJpaEntity staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal no encontrado"));

        if (!institutionId.equals(staff.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre personal de otra institución.");
        }

        // 1. Guardar en la tabla histórica
        StaffHistoryJpaEntity historico = new StaffHistoryJpaEntity(
                staff.getId(),
                institutionId,
                staff.getFirstName(),
                staff.getLastName(),
                staff.getEmail(),
                staff.getRole() != null ? staff.getRole().name() : "STAFF",
                staff.getHireDate(),
                LocalDate.now()
        );
        staffHistoryRepository.save(historico);

        // 2. Limpiar registros relacionados para no romper la foreign key de MySQL
        var portfolios = portfolioRepository.findAllByTenantIdAndStaffIdOrderByActivityDateDesc(institutionId, staff.getId());
        if (portfolios != null && !portfolios.isEmpty()) {
            portfolioRepository.deleteAll(portfolios);
        }

        if (staff.getAssignments() != null) {
            staff.getAssignments().clear();
            staffRepository.saveAndFlush(staff);
        }

        // 3. Eliminar el registro del personal
        staffRepository.delete(staff);
    }
}