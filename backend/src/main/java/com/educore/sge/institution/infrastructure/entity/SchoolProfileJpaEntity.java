package com.educore.sge.institution.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_profiles")
public class SchoolProfileJpaEntity {

    @Id
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(name = "cutoff_date", nullable = false)
    private LocalDate cutoffDate;

    @Column(length = 50)
    private String cue;

    @Column(length = 50)
    private String sector;

    @Column(length = 50)
    private String levels;

    @Column(name = "legal_name", length = 150)
    private String legalName;

    @Column(length = 100)
    private String district;

    @Column(length = 50)
    private String dipregep;

    @Column(length = 50)
    private String shifts;

    @Column(length = 150)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    public LocalDate getCutoffDate() {
        return cutoffDate;
    }

    public void setCutoffDate(LocalDate cutoffDate) {
        this.cutoffDate = cutoffDate;
    }
}