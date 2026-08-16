package com.educore.sge.institution.infrastructure.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FeeService {

    private final FeeJpaRepository feeRepository;

    public FeeService(FeeJpaRepository feeRepository) {
        this.feeRepository = feeRepository;
    }

    @Transactional
    public List<FeeJpaEntity> getStudentFees(String studentId, int academicYear) {
        List<FeeJpaEntity> list = feeRepository.findByStudentIdAndAcademicYear(studentId, academicYear);
        if (list.isEmpty()) {
            return initializeStudentFees(studentId, academicYear);
        }
        return list;
    }

    @Transactional
    public List<FeeJpaEntity> initializeStudentFees(String studentId, int academicYear) {
        List<FeeJpaEntity> fees = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. Matrícula (mes 0)
        FeeJpaEntity matricula = new FeeJpaEntity();
        matricula.setId(UUID.randomUUID().toString());
        matricula.setStudentId(studentId);
        matricula.setAcademicYear(academicYear);
        matricula.setFeeType(FeeType.MATRICULA);
        matricula.setMonthNumber(0);
        matricula.setDueDate(LocalDate.of(academicYear, 2, 28));
        matricula.setStatus(today.isAfter(LocalDate.of(academicYear, 2, 28)) ? FeeStatus.OVERDUE : FeeStatus.PENDING);
        // Dejamos los correos en null
        matricula.setReceiptEmail(null);
        matricula.setQueryEmail(null);
        fees.add(matricula);

        // 2. Cuotas de Marzo a Diciembre (3 al 12)
        for (int month = 3; month <= 12; month++) {
            FeeJpaEntity cuota = new FeeJpaEntity();
            cuota.setId(UUID.randomUUID().toString());
            cuota.setStudentId(studentId);
            cuota.setAcademicYear(academicYear);
            cuota.setFeeType(FeeType.MENSUALIDAD);
            cuota.setMonthNumber(month);
            LocalDate dueDate = LocalDate.of(academicYear, month, 10);
            cuota.setDueDate(dueDate);

            if (today.getYear() == academicYear && today.getMonthValue() == month) {
                cuota.setStatus(today.getDayOfMonth() <= 10 ? FeeStatus.PENDING : FeeStatus.OVERDUE);
            } else if (today.getYear() > academicYear || (today.getYear() == academicYear && today.getMonthValue() > month)) {
                cuota.setStatus(FeeStatus.OVERDUE);
            } else {
                cuota.setStatus(FeeStatus.INACTIVE);
            }

            // Dejamos los correos en null
            cuota.setReceiptEmail(null);
            cuota.setQueryEmail(null);
            fees.add(cuota);
        }

        return feeRepository.saveAll(fees);
    }

    @Transactional
    public FeeJpaEntity toggleFeePayment(String studentId, int academicYear, int monthNumber, String adminUserId) {
        FeeJpaEntity fee = feeRepository.findByStudentIdAndAcademicYearAndMonthNumber(studentId, academicYear, monthNumber)
                .orElseGet(() -> {
                    FeeJpaEntity newFee = new FeeJpaEntity();
                    newFee.setId(UUID.randomUUID().toString());
                    newFee.setStudentId(studentId);
                    newFee.setAcademicYear(academicYear);
                    newFee.setFeeType(monthNumber == 0 ? FeeType.MATRICULA : FeeType.MENSUALIDAD);
                    newFee.setMonthNumber(monthNumber);
                    newFee.setStatus(FeeStatus.INACTIVE);
                    return newFee;
                });

        if (fee.getStatus() == FeeStatus.PAID) {
            LocalDate today = LocalDate.now();
            LocalDate dueDate = fee.getDueDate() != null ? fee.getDueDate() : LocalDate.of(academicYear, Math.max(monthNumber, 1), 10);

            if (today.isAfter(dueDate)) {
                fee.setStatus(FeeStatus.OVERDUE);
            } else if (today.getMonthValue() == monthNumber) {
                fee.setStatus(FeeStatus.PENDING);
            } else {
                fee.setStatus(FeeStatus.INACTIVE);
            }
            fee.setPaymentDate(null);
            fee.setRegisteredByUserId(null);
        } else {
            fee.setStatus(FeeStatus.PAID);
            fee.setPaymentDate(LocalDateTime.now());
            fee.setRegisteredByUserId(adminUserId);
        }

        return feeRepository.save(fee);
    }

    @Transactional
    public void updateContactEmails(String studentId, int academicYear, String receiptEmail, String queryEmail) {
        List<FeeJpaEntity> list = feeRepository.findByStudentIdAndAcademicYear(studentId, academicYear);
        for (FeeJpaEntity fee : list) {
            if (receiptEmail != null) fee.setReceiptEmail(receiptEmail);
            if (queryEmail != null) fee.setQueryEmail(queryEmail);
        }
        feeRepository.saveAll(list);
    }
}