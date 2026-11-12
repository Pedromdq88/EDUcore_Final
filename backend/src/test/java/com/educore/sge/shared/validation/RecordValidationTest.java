package com.educore.sge.shared.validation;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class RecordValidationTest {
    @Test void normalizesEmailAndDni() {
        assertEquals("test@example.com", RecordValidation.requiredEmail(" TEST@Example.COM ", "test"));
        assertEquals("12345678", RecordValidation.comparableDni("12.345.678"));
        assertEquals("", RecordValidation.comparableDni("SIN DNI"));
    }

    @Test void rejectsInvalidAndMissingEmail() {
        assertThrows(ResponseStatusException.class, () -> RecordValidation.requiredEmail("", "cuotas"));
        assertThrows(ResponseStatusException.class, () -> RecordValidation.requiredEmail("sin-arroba", "cuotas"));
    }

    @Test void checksRequiredText() {
        assertThrows(ResponseStatusException.class, () -> RecordValidation.requiredText("  ", "Nombre", 100));
        assertEquals("Ana", RecordValidation.requiredText(" Ana ", "Nombre", 100));
    }
}
