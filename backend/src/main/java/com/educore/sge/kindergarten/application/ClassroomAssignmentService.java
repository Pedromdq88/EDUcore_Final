package com.educore.sge.kindergarten.application;

import com.educore.sge.institution.infrastructure.entity.ClassroomConfigJpaEntity;
import com.educore.sge.institution.infrastructure.repository.ClassroomConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ClassroomAssignmentService {

    @Autowired
    private ClassroomConfigRepository classroomConfigRepository;

    public String calculateClassroom(LocalDate birthDate, Integer academicYear, String tenantId) {
        if (birthDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento es obligatoria.");
        }

        int year = (academicYear != null) ? academicYear : LocalDate.now().getYear();
        int birthYear = birthDate.getYear();
        int birthMonth = birthDate.getMonthValue();

        // 🟢 TU CÁLCULO ORIGINAL INTACTO AL 30 DE JUNIO
        int edadAlCorte = (birthMonth >= 7)
                ? (year - birthYear - 1)
                : (year - birthYear);

        // Buscamos las aulas configuradas en el gestor para esta institución
        List<ClassroomConfigJpaEntity> aulasConfiguradas = classroomConfigRepository.findByTenantId(tenantId);

        if (aulasConfiguradas == null || aulasConfiguradas.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No hay aulas configuradas en el gestor de la institución."
            );
        }

        // Buscamos el aula que coincida con la edad al corte
        ClassroomConfigJpaEntity aulaMatch = aulasConfiguradas.stream()
                .filter(c -> c.getMinimumAge() != null && c.getMinimumAge() == edadAlCorte)
                .findFirst()
                .orElse(null);

        if (aulaMatch != null) {
            return aulaMatch.getName(); // Retorna el nombre configurado (ej: "Sala Naranja", "1° Grado", etc.)
        }

        // Si no hay aula para esa edad, lanzamos error respetando tus rangos
        int minEdad = aulasConfiguradas.stream().mapToInt(ClassroomConfigJpaEntity::getMinimumAge).min().orElse(3);

        if (edadAlCorte < minEdad) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El alumno tiene " + edadAlCorte + " años al 30/06. La edad mínima requerida es de " + minEdad + " años."
            );
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El alumno tiene " + edadAlCorte + " años al 30/06. No hay un aula configurada para esta edad en el gestor."
            );
        }
    }
}