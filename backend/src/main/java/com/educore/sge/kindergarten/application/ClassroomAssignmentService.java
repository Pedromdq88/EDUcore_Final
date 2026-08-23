package com.educore.sge.kindergarten.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class ClassroomAssignmentService {

    public String calculateClassroom(LocalDate birthDate, Integer academicYear) {
        if (birthDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento es obligatoria.");
        }

        int year = (academicYear != null) ? academicYear : LocalDate.now().getYear();
        int birthYear = birthDate.getYear();
        int birthMonth = birthDate.getMonthValue();


        int edadAlCorte = (birthMonth >= 7)
                ? (year - birthYear - 1)
                : (year - birthYear);

        return switch (edadAlCorte) {
            case 3 -> "1° Sección (3 años)";
            case 4 -> "2° Sección (4 años)";
            case 5 -> "3° Sección (5 años)";
            default -> {
                if (edadAlCorte < 3) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El alumno tiene " + edadAlCorte + " años al 30/06. La edad mínima de ingreso es de 3 años cumplidos al 30 de junio (1° Sección)."
                    );
                } else {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El alumno tiene " + edadAlCorte + " años al 30/06. Corresponde a Nivel Primario (máximo 5 años para Nivel Inicial)."
                    );
                }
            }
        };
    }
}