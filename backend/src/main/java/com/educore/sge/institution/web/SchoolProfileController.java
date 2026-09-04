package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.SchoolChangeTicketJpaEntity;
import com.educore.sge.institution.infrastructure.entity.SchoolProfileJpaEntity;
import com.educore.sge.institution.infrastructure.entity.TenantJpaEntity;
import com.educore.sge.institution.infrastructure.repository.SchoolChangeTicketJpaRepository;
import com.educore.sge.institution.infrastructure.repository.SchoolProfileJpaRepository;
import com.educore.sge.institution.infrastructure.repository.TenantJpaRepository;
import com.educore.sge.institution.service.ClassroomCatalogService;
import com.educore.sge.shared.domain.EducationLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping("/api/v1/institution/profile")
public class SchoolProfileController {

    private final SchoolProfileJpaRepository profileRepository;
    private final TenantJpaRepository tenantRepository;
    private final SchoolChangeTicketJpaRepository ticketRepository;
    private final ClassroomCatalogService classroomCatalogService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public SchoolProfileController(SchoolProfileJpaRepository profileRepository,
                                   TenantJpaRepository tenantRepository,
                                   SchoolChangeTicketJpaRepository ticketRepository,
                                   ClassroomCatalogService classroomCatalogService) {
        this.profileRepository = profileRepository;
        this.tenantRepository = tenantRepository;
        this.ticketRepository = ticketRepository;
        this.classroomCatalogService = classroomCatalogService;
    }

    @GetMapping
    public Map<String, Object> getProfile(@RequestHeader("X-Institution-Id") String institutionId) {
        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cabecera X-Institution-Id requerida");
        }

        TenantJpaEntity tenant = tenantRepository.findById(institutionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Institución no registrada con ID: " + institutionId
                ));

        SchoolProfileJpaEntity profile = profileRepository.findById(institutionId).orElseGet(() -> {
            int currentYear = LocalDate.now().getYear();

            SchoolProfileJpaEntity newProfile = new SchoolProfileJpaEntity();
            newProfile.setTenantId(tenant.getId());
            newProfile.setName(tenant.getName());
            newProfile.setCue(tenant.getCueCode());
            newProfile.setAcademicYear(currentYear);
            newProfile.setCutoffDate(LocalDate.of(currentYear, 6, 30));

            return profileRepository.save(newProfile);
        });

        EducationLevel level = tenant.getEducationLevel();

        return Map.of(
                "profile", profile,
                "educationLevel", level != null ? level.name() : "JARDIN",
                "levelDisplayName", classroomCatalogService.getDisplayName(level),
                "classrooms", classroomCatalogService.getClassroomsForLevel(level)
        );
    }

    @Transactional
    @PutMapping
    public SchoolProfileJpaEntity updateProfile(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody SchoolProfileJpaEntity updated) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cabecera X-Institution-Id requerida");
        }

        updated.setTenantId(institutionId);
        if (updated.getAcademicYear() != null) {
            updated.setCutoffDate(LocalDate.of(updated.getAcademicYear(), 6, 30));
        }

        return profileRepository.save(updated);
    }

    @GetMapping("/tickets")
    public List<SchoolChangeTicketJpaEntity> getPendingTickets(
            @RequestHeader("X-Institution-Id") String institutionId) {
        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cabecera X-Institution-Id requerida");
        }
        return ticketRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(institutionId, "PENDING");
    }

    @Transactional
    @PostMapping("/tickets")
    public ResponseEntity<?> createTicket(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestHeader(value = "X-User-Email", defaultValue = "admin@institucion.com") String userEmail,
            @RequestBody Map<String, Object> body) {

        String reason = (String) body.get("reason");
        Object proposedData = body.get("proposedData");

        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar el motivo de la modificación.");
        }

        try {
            SchoolChangeTicketJpaEntity ticket = new SchoolChangeTicketJpaEntity();
            ticket.setId(UUID.randomUUID().toString());
            ticket.setTenantId(institutionId);
            ticket.setRequestedByEmail(userEmail);
            ticket.setReason(reason);
            ticket.setProposedDataJson(objectMapper.writeValueAsString(proposedData));
            ticket.setStatus("PENDING");
            ticket.setCreatedAt(LocalDateTime.now());

            return ResponseEntity.ok(ticketRepository.save(ticket));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al serializar ticket");
        }
    }

    @Transactional
    @PostMapping("/tickets/{ticketId}/resolve")
    public ResponseEntity<?> resolveTicket(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String ticketId,
            @RequestParam boolean approve) {

        SchoolChangeTicketJpaEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        if (!institutionId.equals(ticket.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No pertenece a esta institución.");
        }

        if (approve) {
            try {
                SchoolProfileJpaEntity entity = objectMapper.readValue(ticket.getProposedDataJson(), SchoolProfileJpaEntity.class);
                entity.setTenantId(institutionId);
                if (entity.getAcademicYear() != null) {
                    entity.setCutoffDate(LocalDate.of(entity.getAcademicYear(), 6, 30));
                }
                profileRepository.save(entity);
                ticket.setStatus("APPROVED");
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al aplicar datos del ticket.");
            }
        } else {
            ticket.setStatus("REJECTED");
        }

        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        return ResponseEntity.ok(Map.of("message", approve ? "Ticket aprobado" : "Ticket rechazado"));
    }
}