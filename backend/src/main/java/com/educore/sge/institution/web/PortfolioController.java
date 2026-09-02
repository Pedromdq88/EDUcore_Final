package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.PortfolioJpaEntity;
import com.educore.sge.institution.infrastructure.repository.PortfolioJpaRepository;
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
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioJpaRepository portfolioRepository;

    public PortfolioController(PortfolioJpaRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER', 'TUTOR')")
    @GetMapping
    public List<PortfolioJpaEntity> getPortfolios(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) String classroom,
            @RequestParam(required = false) String studentId) {

        if (staffId != null && !staffId.isBlank()) {
            return portfolioRepository.findAllByTenantIdAndStaffIdOrderByActivityDateDesc(institutionId, staffId);
        }
        if (studentId != null && !studentId.isBlank()) {
            return portfolioRepository.findAllByTenantIdAndStudentIdOrderByActivityDateDesc(institutionId, studentId);
        }
        if (classroom != null && !classroom.isBlank() && !classroom.equalsIgnoreCase("TODAS")) {
            return portfolioRepository.findAllByTenantIdAndClassroomOrderByActivityDateDesc(institutionId, classroom);
        }
        return portfolioRepository.findAllByTenantIdOrderByActivityDateDesc(institutionId);
    }

    // Crear nueva evidencia / entrada de portfolio
    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PostMapping
    public PortfolioJpaEntity createPortfolio(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody PortfolioJpaEntity portfolio) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta cabecera X-Institution-Id");
        }

        if (portfolio.getId() == null || portfolio.getId().isBlank()) {
            portfolio.setId(UUID.randomUUID().toString());
        }
        portfolio.setTenantId(institutionId);

        if (portfolio.getActivityDate() == null) {
            portfolio.setActivityDate(LocalDate.now());
        }
        if (portfolio.getCategory() == null || portfolio.getCategory().isBlank()) {
            portfolio.setCategory("CURSO");
        }

        return portfolioRepository.save(portfolio);
    }

    // Editar entrada de portfolio
    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PutMapping("/{id}")
    public PortfolioJpaEntity updatePortfolio(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            @RequestBody PortfolioJpaEntity updated) {

        PortfolioJpaEntity existing = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio no encontrado"));

        if (!institutionId.equals(existing.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre registros de otra institución.");
        }

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setClassroom(updated.getClassroom());
        existing.setStudentId(updated.getStudentId());
        existing.setAssignmentId(updated.getAssignmentId());
        existing.setMediaUrl(updated.getMediaUrl());
        if (updated.getActivityDate() != null) {
            existing.setActivityDate(updated.getActivityDate());
        }

        return portfolioRepository.save(existing);
    }

    // Eliminar entrada de portfolio
    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @DeleteMapping("/{id}")
    public void deletePortfolio(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {

        PortfolioJpaEntity existing = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio no encontrado"));

        if (!institutionId.equals(existing.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar registros de otra institución.");
        }

        portfolioRepository.delete(existing);
    }
}