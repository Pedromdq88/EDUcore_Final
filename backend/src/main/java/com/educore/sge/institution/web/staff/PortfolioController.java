package com.educore.sge.institution.web.staff;

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

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioJpaRepository portfolioRepository;
    private final com.educore.sge.auth.RoleScopeService scopes;

    public PortfolioController(PortfolioJpaRepository portfolioRepository, com.educore.sge.auth.RoleScopeService scopes) {
        this.portfolioRepository = portfolioRepository; this.scopes = scopes;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping
    public List<PortfolioJpaEntity> getPortfolios(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestParam(required = false) String staffId,
            org.springframework.security.core.Authentication authentication) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta cabecera X-Institution-Id");
        }

        if(scopes.is(authentication,"TEACHER")) {
            String self=scopes.teacherStaffId(authentication,institutionId);
            if(staffId!=null && !staffId.isBlank() && !self.equals(staffId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo tu propio portfolio");
            return portfolioRepository.findAllByTenantIdAndStaffIdOrderByActivityDateDesc(institutionId,self);
        }
        if(scopes.is(authentication,"PRECEPTOR")) return List.of();
        if (staffId != null && !staffId.isBlank()) {
            return portfolioRepository.findAllByTenantIdAndStaffIdOrderByActivityDateDesc(institutionId, staffId);
        }
        return portfolioRepository.findAllByTenantIdOrderByActivityDateDesc(institutionId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @PostMapping
    public PortfolioJpaEntity createPortfolio(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody PortfolioJpaEntity portfolio,
            org.springframework.security.core.Authentication authentication) {

        if (institutionId == null || institutionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta cabecera X-Institution-Id");
        }

        if(scopes.is(authentication,"TEACHER") &&
           !scopes.teacherStaffId(authentication,institutionId).equals(portfolio.getStaffId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Portfolio de otro docente");
        if (portfolio.getStaffId() == null || portfolio.getStaffId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del docente (staffId) es obligatorio");
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

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @PutMapping("/{id}")
    public PortfolioJpaEntity updatePortfolio(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            @RequestBody PortfolioJpaEntity updated,
            org.springframework.security.core.Authentication authentication) {

        PortfolioJpaEntity existing = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio no encontrado"));

        if (!institutionId.equals(existing.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a esta institución.");
        }

        if(scopes.is(authentication,"TEACHER") &&
            !scopes.teacherStaffId(authentication,institutionId).equals(existing.getStaffId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Portfolio de otro docente");
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setMediaUrl(updated.getMediaUrl());
        if (updated.getActivityDate() != null) {
            existing.setActivityDate(updated.getActivityDate());
        }

        return portfolioRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @DeleteMapping("/{id}")
    public void deletePortfolio(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            org.springframework.security.core.Authentication authentication) {

        PortfolioJpaEntity existing = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio no encontrado"));

        if (!institutionId.equals(existing.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a esta institución.");
        }

        if(scopes.is(authentication,"TEACHER") &&
            !scopes.teacherStaffId(authentication,institutionId).equals(existing.getStaffId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Portfolio de otro docente");
        portfolioRepository.delete(existing);
    }
}