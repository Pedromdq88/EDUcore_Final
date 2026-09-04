package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.TenantJpaEntity;
import com.educore.sge.institution.infrastructure.repository.TenantJpaRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/public/institutions")
public class TenantController {

    private final TenantJpaRepository tenantRepository;

    public TenantController(TenantJpaRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    public List<TenantJpaEntity> getInstitutions() {
        return tenantRepository.findAll();
    }
}