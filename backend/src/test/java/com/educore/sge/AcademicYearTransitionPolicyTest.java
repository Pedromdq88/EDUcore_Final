package com.educore.sge.institution.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class AcademicYearTransitionPolicyTest {
    @Test void impideAvanzarAntesDelDoceDeNoviembre() {
        assertThrows(ResponseStatusException.class, () ->
            AcademicYearTransitionPolicy.validate(2027, 2028, LocalDate.of(2027, 11, 11)));
    }

    @Test void habilitaAvanzarElDoceDeNoviembre() {
        assertDoesNotThrow(() ->
            AcademicYearTransitionPolicy.validate(2027, 2028, LocalDate.of(2027, 11, 12)));
    }

    @Test void rechazaSaltosDeDosCiclos() {
        assertThrows(ResponseStatusException.class, () ->
            AcademicYearTransitionPolicy.validate(2027, 2029, LocalDate.of(2028, 11, 12)));
    }

    @Test void rechazaRetroceder() {
        assertThrows(ResponseStatusException.class, () ->
            AcademicYearTransitionPolicy.validate(2027, 2026, LocalDate.of(2027, 11, 12)));
    }

    @Test void permiteActualizarDatosSinCambiarElCiclo() {
        assertDoesNotThrow(() ->
            AcademicYearTransitionPolicy.validate(2027, 2027, LocalDate.of(2026, 11, 12)));
    }
}
