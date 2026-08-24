package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.StaffJpaEntity;
import com.educore.sge.institution.infrastructure.repository.StaffJpaRepository;
import com.educore.sge.institution.persistence.StaffHistoryJpaEntity;
import com.educore.sge.institution.persistence.StaffHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping("/api/v1/institution/staff")
public class StaffController {

    private final StaffJpaRepository staffRepository;
    private final StaffHistoryRepository staffHistoryRepository;

    public StaffController(StaffJpaRepository staffRepository, StaffHistoryRepository staffHistoryRepository) {
        this.staffRepository = staffRepository;
        this.staffHistoryRepository = staffHistoryRepository;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping
    public List<StaffJpaEntity> getAllStaff(
            @RequestHeader(value = "X-Institution-Id", defaultValue = "88888888-4444-4444-4444-121212121212") String institutionId) {
        return staffRepository.findAllByTenantId(institutionId);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping("/{id}")
    public StaffJpaEntity getStaffById(@PathVariable String id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro del personal no encontrado"));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping
    public StaffJpaEntity createStaff(
            @RequestHeader(value = "X-Institution-Id", defaultValue = "88888888-4444-4444-4444-121212121212") String institutionId,
            @RequestBody StaffJpaEntity staff) {

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

        return staffRepository.save(staff);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PutMapping("/{id}")
    public StaffJpaEntity updateStaff(
            @RequestHeader(value = "X-Institution-Id", defaultValue = "88888888-4444-4444-4444-121212121212") String institutionId,
            @PathVariable String id,
            @RequestBody StaffJpaEntity updated) {

        StaffJpaEntity existing = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro del personal no encontrado"));

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setRole(updated.getRole());
        existing.setClassroom(updated.getClassroom());

        if (updated.getDocumentNumber() != null) existing.setDocumentNumber(updated.getDocumentNumber());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        if (updated.getHireDate() != null) existing.setHireDate(updated.getHireDate());

        return staffRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{id}/baja")
    public void darDeBajaStaff(@PathVariable String id) {
        StaffJpaEntity staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro del personal no encontrado"));

        StaffHistoryJpaEntity historico = new StaffHistoryJpaEntity(
                staff.getId(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getEmail(),
                staff.getRole() != null ? staff.getRole().name() : "STAFF",
                staff.getHireDate(),
                LocalDate.now()
        );
        staffHistoryRepository.save(historico);

        staffRepository.delete(staff);
    }
}