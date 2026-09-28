package com.educore.sge.institution.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "student_medical_records")
@Data
public class StudentMedicalRecord {

    @Id
    private String id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Información de Salud / Cobertura
    private String obraSocial;
    private String detalleObraSocial;
    private String nroAfiliado;

    // Antecedentes Personales
    private boolean asma;
    private boolean alergiaGeneral;
    private boolean problemasCardiacos;
    private boolean diabetes;
    private boolean presionArterialElevada;
    private boolean convulsiones;
    private boolean alteracionesSanguineas;
    private boolean quemadurasSeveras;
    private boolean faltaOrgano;
    private boolean enfermedadOncohematologica;
    private boolean inmunodeficiencias;
    private boolean fracturasLesiones;
    private boolean otroProblemaHuesos;
    private boolean traumatismoCraneo;
    private boolean problemasPiel;

    // Ejercicio
    private boolean desmayos;
    private boolean dolorPecho;
    private boolean mareos;
    private boolean mayorCansancio;
    private boolean palpitaciones;
    private boolean dificultadRespirar;

    // Internaciones y Operaciones
    private boolean internacionSalaComun;
    private boolean internacionTerapia;
    @Column(columnDefinition = "TEXT")
    private String detalleInternacion;
    private boolean operacion;
    @Column(columnDefinition = "TEXT")
    private String motivoOperacion;
    private String anioOperacion;

    // Alergias Graves
    private boolean alergiaMedicamentos;
    private boolean internacionMed;
    private boolean alergiaVacunas;
    private boolean internacionVac;
    private boolean alergiaAlimentos;
    private boolean internacionAlim;
    private boolean alergiaInsectos;
    private boolean internacionIns;
    private boolean alergiaEstacionales;
    private boolean internacionEst;
    private boolean alergiaOtras;
    private boolean internacionOtr;

    // Discapacidades y Medicación
    private boolean disminucionAuditiva;
    private boolean usaAudifonos;
    private boolean disminucionVisual;
    private boolean usaLentes;
    private boolean medicacionHabitual;
    @Column(columnDefinition = "TEXT")
    private String cualMedicacion;

    // Antecedentes Familiares
    private boolean muerteSubitaFamiliar;
    private boolean diabetesFamiliar;
    private boolean problemasCardiacosFamiliar;
    private boolean tosCronicaFamiliar;
    private boolean celiaquiaFamiliar;
}