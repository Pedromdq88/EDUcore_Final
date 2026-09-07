package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.SchoolProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolProfileJpaRepository extends JpaRepository<SchoolProfileJpaEntity, String> {
}