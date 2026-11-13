package com.educore.sge.auth;

import com.educore.sge.institution.infrastructure.entity.PortfolioJpaEntity;
import com.educore.sge.institution.infrastructure.repository.PortfolioJpaRepository;
import com.educore.sge.institution.infrastructure.repository.StaffJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile/staff/portfolios")
@PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR','TEACHER')")
public class MyStaffPortfolioController {
    private static final Set<String> CATEGORIES = Set.of("CURSO", "CAPACITACION", "PROYECTO");
    private final PortfolioJpaRepository portfolios;
    private final AuthUserRepository accounts;
    private final StaffJpaRepository staff;

    public MyStaffPortfolioController(PortfolioJpaRepository portfolios,
                                      AuthUserRepository accounts,
                                      StaffJpaRepository staff) {
        this.portfolios = portfolios;
        this.accounts = accounts;
        this.staff = staff;
    }

    public record PortfolioInput(String title, String description, String category,
                                 LocalDate activityDate, String mediaUrl) {}

    private String ownStaffId(Authentication authentication, String tenantId) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var account = accounts.findByTenantIdAndEmailIgnoreCase(tenantId, authentication.getName())
            .filter(a -> a.enabled && a.staffId != null && !a.staffId.isBlank())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta sin legajo de personal"));
        var person = staff.findById(account.staffId)
            .filter(s -> tenantId.equals(s.getTenantId()) && "ACTIVE".equalsIgnoreCase(s.getStatus()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Legajo no habilitado"));
        return person.getId();
    }

    private PortfolioJpaEntity ownPortfolio(String id, String tenantId, String staffId) {
        return portfolios.findById(id)
            .filter(p -> tenantId.equals(p.getTenantId()) && staffId.equals(p.getStaffId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio no encontrado"));
    }

    private void apply(PortfolioJpaEntity target, PortfolioInput data) {
        if (data == null || data.title() == null || data.title().isBlank() || data.title().trim().length() > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título inválido");
        if (data.description() == null || data.description().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descripción obligatoria");
        String category = data.category() == null || data.category().isBlank()
            ? "CURSO" : data.category().trim();
        if (!CATEGORIES.contains(category))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría inválida");
        String url = data.mediaUrl() == null ? "" : data.mediaUrl().trim();
        if (url.length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL demasiado larga");
        target.setTitle(data.title().trim());
        target.setDescription(data.description().trim());
        target.setCategory(category);
        target.setMediaUrl(url.isEmpty() ? null : url);
        target.setActivityDate(data.activityDate() == null ? LocalDate.now() : data.activityDate());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PortfolioJpaEntity> list(@RequestHeader("X-Institution-Id") String tenantId,
                                         Authentication authentication) {
        String staffId = ownStaffId(authentication, tenantId);
        return portfolios.findAllByTenantIdAndStaffIdOrderByActivityDateDesc(tenantId, staffId);
    }

    @PostMapping
    @Transactional
    public PortfolioJpaEntity create(@RequestHeader("X-Institution-Id") String tenantId,
                                     Authentication authentication,
                                     @RequestBody PortfolioInput data) {
        String staffId = ownStaffId(authentication, tenantId);
        var item = new PortfolioJpaEntity();
        item.setId(UUID.randomUUID().toString());
        item.setTenantId(tenantId);
        item.setStaffId(staffId);
        apply(item, data);
        return portfolios.save(item);
    }

    @PutMapping("/{id}")
    @Transactional
    public PortfolioJpaEntity update(@RequestHeader("X-Institution-Id") String tenantId,
                                     Authentication authentication, @PathVariable String id,
                                     @RequestBody PortfolioInput data) {
        String staffId = ownStaffId(authentication, tenantId);
        var item = ownPortfolio(id, tenantId, staffId);
        apply(item, data);
        return portfolios.save(item);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@RequestHeader("X-Institution-Id") String tenantId,
                       Authentication authentication, @PathVariable String id) {
        String staffId = ownStaffId(authentication, tenantId);
        portfolios.delete(ownPortfolio(id, tenantId, staffId));
    }
}
