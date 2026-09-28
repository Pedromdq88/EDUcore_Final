package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.StudentMedicalRecord;
import com.educore.sge.institution.infrastructure.repository.StudentMedicalRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@CrossOrigin(origins = "*")
public class StudentMedicalRecordController {

    @Autowired
    private StudentMedicalRecordRepository medicalRepository;

    @GetMapping("/{studentId}/medical-record")
    public ResponseEntity<StudentMedicalRecord> getMedicalRecord(
            @PathVariable String studentId,
            @RequestHeader("X-Institution-Id") String tenantId) {

        StudentMedicalRecord record = medicalRepository.findByStudentIdAndTenantId(studentId, tenantId)
                .orElse(new StudentMedicalRecord());
        return ResponseEntity.ok(record);
    }

    @PostMapping("/{studentId}/medical-record")
    public ResponseEntity<?> saveMedicalRecord(
            @PathVariable String studentId,
            @RequestHeader("X-Institution-Id") String tenantId,
            @RequestHeader(value = "X-User-Role", defaultValue = "TEACHER") String userRole,
            @RequestBody StudentMedicalRecord incoming) {

        // Validación estricta de roles institucionales
        if (!"DIRECTOR".equals(userRole) && !"ADMINISTRATIVE".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado: Solo Dirección y Administración pueden registrar o modificar fichas médicas.");
        }

        StudentMedicalRecord record = medicalRepository.findByStudentIdAndTenantId(studentId, tenantId)
                .orElse(new StudentMedicalRecord());

        if (record.getId() == null) {
            record.setId(UUID.randomUUID().toString());
            record.setStudentId(studentId);
            record.setTenantId(tenantId);
        }

        // Mapeo completo de todos los campos de las planillas oficiales
        record.setObraSocial(incoming.getObraSocial());
        record.setDetalleObraSocial(incoming.getDetalleObraSocial());
        record.setNroAfiliado(incoming.getNroAfiliado());

        record.setAsma(incoming.isAsma());
        record.setAlergiaGeneral(incoming.isAlergiaGeneral());
        record.setProblemasCardiacos(incoming.isProblemasCardiacos());
        record.setDiabetes(incoming.isDiabetes());
        record.setPresionArterialElevada(incoming.isPresionArterialElevada());
        record.setConvulsiones(incoming.isConvulsiones());
        record.setAlteracionesSanguineas(incoming.isAlteracionesSanguineas());
        record.setQuemadurasSeveras(incoming.isQuemadurasSeveras());
        record.setFaltaOrgano(incoming.isFaltaOrgano());
        record.setEnfermedadOncohematologica(incoming.isEnfermedadOncohematologica());
        record.setInmunodeficiencias(incoming.isInmunodeficiencias());
        record.setFracturasLesiones(incoming.isFracturasLesiones());
        record.setOtroProblemaHuesos(incoming.isOtroProblemaHuesos());
        record.setTraumatismoCraneo(incoming.isTraumatismoCraneo());
        record.setProblemasPiel(incoming.isProblemasPiel());

        record.setDesmayos(incoming.isDesmayos());
        record.setDolorPecho(incoming.isDolorPecho());
        record.setMareos(incoming.isMareos());
        record.setMayorCansancio(incoming.isMayorCansancio());
        record.setPalpitaciones(incoming.isPalpitaciones());
        record.setDificultadRespirar(incoming.isDificultadRespirar());

        record.setInternacionSalaComun(incoming.isInternacionSalaComun());
        record.setInternacionTerapia(incoming.isInternacionTerapia());
        record.setDetalleInternacion(incoming.getDetalleInternacion());
        record.setOperacion(incoming.isOperacion());
        record.setMotivoOperacion(incoming.getMotivoOperacion());
        record.setAnioOperacion(incoming.getAnioOperacion());

        record.setAlergiaMedicamentos(incoming.isAlergiaMedicamentos());
        record.setInternacionMed(incoming.isInternacionMed());
        record.setAlergiaVacunas(incoming.isAlergiaVacunas());
        record.setInternacionVac(incoming.isInternacionVac());
        record.setAlergiaAlimentos(incoming.isAlergiaAlimentos());
        record.setInternacionAlim(incoming.isInternacionAlim());
        record.setAlergiaInsectos(incoming.isAlergiaInsectos());
        record.setInternacionIns(incoming.isInternacionIns());
        record.setAlergiaEstacionales(incoming.isAlergiaEstacionales());
        record.setInternacionEst(incoming.isInternacionEst());
        record.setAlergiaOtras(incoming.isAlergiaOtras());
        record.setInternacionOtr(incoming.isInternacionOtr());

        record.setDisminucionAuditiva(incoming.isDisminucionAuditiva());
        record.setUsaAudifonos(incoming.isUsaAudifonos());
        record.setDisminucionVisual(incoming.isDisminucionVisual());
        record.setUsaLentes(incoming.isUsaLentes());
        record.setMedicacionHabitual(incoming.isMedicacionHabitual());
        record.setCualMedicacion(incoming.getCualMedicacion());

        record.setMuerteSubitaFamiliar(incoming.isMuerteSubitaFamiliar());
        record.setDiabetesFamiliar(incoming.isDiabetesFamiliar());
        record.setProblemasCardiacosFamiliar(incoming.isProblemasCardiacosFamiliar());
        record.setTosCronicaFamiliar(incoming.isTosCronicaFamiliar());
        record.setCeliaquiaFamiliar(incoming.isCeliaquiaFamiliar());

        StudentMedicalRecord saved = medicalRepository.save(record);
        return ResponseEntity.ok(saved);
    }
}