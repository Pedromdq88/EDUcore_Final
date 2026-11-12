package com.educore.sge.institution.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;

public final class AcademicYearTransitionPolicy {
    private AcademicYearTransitionPolicy() {}

    public static void validate(Integer previousYear, Integer nextYear, LocalDate today) {
        if (nextYear == null || nextYear < 2000 || nextYear > 2100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ciclo lectivo inválido.");
        if (previousYear == null || previousYear.equals(nextYear)) return;
        if (nextYear < previousYear)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede retroceder el ciclo lectivo activo.");
        if (nextYear != previousYear + 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede avanzar un ciclo lectivo por vez.");
        LocalDate enabledOn = LocalDate.of(previousYear, 11, 12);
        if (today.isBefore(enabledOn))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cambio al ciclo " + nextYear + " estará habilitado desde el 12/11/" + previousYear + ".");
    }
}
