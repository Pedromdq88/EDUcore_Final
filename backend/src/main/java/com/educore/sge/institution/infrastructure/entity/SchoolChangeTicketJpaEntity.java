package com.educore.sge.institution.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_change_tickets")
public class SchoolChangeTicketJpaEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "requested_by_email", nullable = false, length = 150)
    private String requestedByEmail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "proposed_data_json", columnDefinition = "TEXT", nullable = false)
    private String proposedDataJson;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}