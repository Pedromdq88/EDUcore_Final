package com.educore.sge.institution.infrastructure.finance;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_fees", uniqueConstraints = {
        @UniqueConstraint(name = "unq_student_fee_tenant", columnNames = {"tenant_id", "student_id", "academic_year", "fee_type", "month_number"})
})
public class FeeJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "student_id", nullable = false, length = 36)
    private String studentId;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 50)
    private FeeType feeType;

    @Column(name = "month_number", nullable = false)
    private Integer monthNumber; // 0 = Matrícula, 3..12 = Marzo..Diciembre

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FeeStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "registered_by_user_id", length = 150)
    private String registeredByUserId;

    @Column(name = "receipt_email", length = 150)
    private String receiptEmail;

    @Column(name = "query_email", length = 150)
    private String queryEmail;

    public FeeJpaEntity() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Integer getAcademicYear() { return academicYear; }
    public void setAcademicYear(Integer academicYear) { this.academicYear = academicYear; }

    public FeeType getFeeType() { return feeType; }
    public void setFeeType(FeeType feeType) { this.feeType = feeType; }

    public Integer getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Integer monthNumber) { this.monthNumber = monthNumber; }

    public FeeStatus getStatus() { return status; }
    public void setStatus(FeeStatus status) { this.status = status; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public String getRegisteredByUserId() { return registeredByUserId; }
    public void setRegisteredByUserId(String registeredByUserId) { this.registeredByUserId = registeredByUserId; }

    public String getReceiptEmail() { return receiptEmail; }
    public void setReceiptEmail(String receiptEmail) { this.receiptEmail = receiptEmail; }

    public String getQueryEmail() { return queryEmail; }
    public void setQueryEmail(String queryEmail) { this.queryEmail = queryEmail; }
}