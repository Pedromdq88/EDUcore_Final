package com.educore.sge.shared.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

/** Errores de reglas de negocio legibles en el formulario Angular. */
@RestControllerAdvice
public class ApiErrorAdvice {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "status", ex.getStatusCode().value(),
            "message", ex.getReason() == null ? "No se pudo completar la operación." : ex.getReason()
        ));
    }
}
