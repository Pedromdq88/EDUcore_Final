package com.educore.sge.academic.application;

import com.educore.sge.academic.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.academic.infrastructure.entity.StudentStatus;
import com.educore.sge.academic.infrastructure.repository.StudentJpaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BajaService {

    private final StudentJpaRepository studentRepository;

    public BajaService(StudentJpaRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Paso 1: Petición de Baja.
     * La puede iniciar una maestra, preceptor o el tutor del alumno.
     * El estado cambia a PENDING_WITHDRAWAL (Pendiente de Aceptación).
     */
    @Transactional
    @PreAuthorize("hasRole('TEACHER')") // Administrativos, Directores y Dueños también entran por jerarquía
    public void requestBaja(String studentId) {
        StudentJpaEntity student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        if (student.getStatus() == StudentStatus.WITHDRAWN) {
            throw new IllegalStateException("El alumno ya se encuentra dado de baja de forma definitiva.");
        }

        student.setStatus(StudentStatus.PENDING_WITHDRAWAL);
        studentRepository.save(student);
    }

    /**
     * Paso 2: Aceptación/Aprobación de la Baja.
     * ESTO SÓLO LO PUEDE HACER LA DIRECTORA O EL DUEÑO.
     * El alumno pasa a WITHDRAWN (Baja definitiva), guardando su historial intacto pero liberando la vacante.
     */
    @Transactional
    @PreAuthorize("hasRole('DIRECTOR')") // El OWNER también puede por jerarquía
    public void approveWithdrawal(String studentId) {
        StudentJpaEntity student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        if (student.getStatus() != StudentStatus.PENDING_WITHDRAWAL) {
            throw new IllegalStateException("No se puede aprobar la baja de un alumno que no tiene una solicitud pendiente.");
        }

        student.setStatus(StudentStatus.WITHDRAWN);
        studentRepository.save(student);
    }
}
