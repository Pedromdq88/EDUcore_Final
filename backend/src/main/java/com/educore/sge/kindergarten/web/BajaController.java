package com.educore.sge.kindergarten.web;

import com.educore.sge.kindergarten.application.BajaService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/students")
public class BajaController {

    private final BajaService bajaService;

    public BajaController(BajaService bajaService) {
        this.bajaService = bajaService;
    }

    /**
     * Paso 1: El tutor, preceptor o maestra pide la baja.
     * El estudiante pasa a estado PENDING_WITHDRAWAL.
     */
    @PostMapping("/{studentId}/request-baja")
    public void pedirBaja(@PathVariable String studentId) {
        bajaService.requestBaja(studentId);
    }

    /**
     * Paso 2: Solo la Directora o el Dueño aprueban la baja permanente.
     * El estudiante pasa a estado WITHDRAWN de forma segura en la tabla sin borrarse.
     */
    @PostMapping("/{studentId}/approve-baja")
    public void aceptarBaja(@PathVariable String studentId) {
        bajaService.approveWithdrawal(studentId);
    }
}
