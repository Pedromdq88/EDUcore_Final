package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.InstitutionJpaEntity;
import com.educore.sge.institution.infrastructure.repository.InstitutionJpaRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {

    private final InstitutionJpaRepository repository;

    public InstitutionController(InstitutionJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<InstitutionJpaEntity> getAllInstitutions() {
        return repository.findAll();
    }
}
