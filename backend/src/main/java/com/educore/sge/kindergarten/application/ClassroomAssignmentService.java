package com.educore.sge.kindergarten.application;

import com.educore.sge.institution.infrastructure.entity.ClassroomConfigJpaEntity;
import com.educore.sge.institution.infrastructure.domain.ClassroomShift;
import com.educore.sge.institution.infrastructure.repository.ClassroomConfigRepository;
import com.educore.sge.institution.infrastructure.repository.SchoolProfileJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ClassroomAssignmentService {
    private final ClassroomConfigRepository classrooms;
    private final SchoolProfileJpaRepository profiles;
    public ClassroomAssignmentService(ClassroomConfigRepository classrooms, SchoolProfileJpaRepository profiles) {
        this.classrooms = classrooms; this.profiles = profiles;
    }

    public record ClassroomOption(String id, String name, Integer minimumAge, ClassroomShift shift) {}
    public record Eligibility(int ageAtCutoff, LocalDate cutoffDate, List<ClassroomOption> options, String message) {}

    public Eligibility eligibleClassrooms(LocalDate birthDate, Integer academicYear, String tenantId, ClassroomShift requestedShift) {
        if (birthDate == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha de nacimiento obligatoria.");
        int activeYear = profiles.findById(tenantId).map(p -> p.getAcademicYear()).orElse(LocalDate.now().getYear());
        int year = academicYear == null ? activeYear : academicYear;
        if (year != activeYear) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Solo está habilitado el ciclo lectivo " + activeYear + " de la institución.");
        if (year < 2000 || year > 2100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ciclo lectivo inválido.");
        LocalDate cutoff = LocalDate.of(year, 6, 30);
        if (birthDate.isAfter(cutoff)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El alumno todavía no nació a la fecha de corte.");
        int age = year - birthDate.getYear() - (birthDate.getMonthValue() >= 7 ? 1 : 0);
        List<ClassroomConfigJpaEntity> all = classrooms.findByTenantId(tenantId);
        List<ClassroomOption> available = all.stream()
            .filter(c -> c.getMinimumAge() != null && (c.getMinimumAge() == age || (age == 2 && c.getMinimumAge() == 1 && c.getName().toLowerCase(java.util.Locale.ROOT).contains("maternal"))))
            .filter(c -> requestedShift == null || requestedShift == ClassroomShift.AMBOS ||
                    c.getShift() == ClassroomShift.AMBOS || c.getShift() == requestedShift)
            .sorted(Comparator.comparing(ClassroomConfigJpaEntity::getName, String.CASE_INSENSITIVE_ORDER))
            .map(c -> new ClassroomOption(c.getId(), c.getName(), c.getMinimumAge(), c.getShift()))
            .toList();
        String message;
        if (!available.isEmpty()) message = available.size() == 1 ? "Se encontró una sala apta para su edad." : "Hay varias salas aptas; elegí una del desplegable.";
        else if (all.isEmpty()) message = "No hay aulas guardadas para esta institución. Creá primero las aulas en Aulas y horarios.";
        else message = "Edad al 30/06: " + age + " años. No existen aulas registradas para esta edad y turno. Verificá Aulas y horarios.";
        return new Eligibility(age, cutoff, available, message);
    }

    public String calculateClassroom(LocalDate birthDate, Integer academicYear, String tenantId) {
        return calculateClassroom(birthDate, academicYear, tenantId, null, null);
    }
    public String calculateClassroom(LocalDate birthDate, Integer academicYear, String tenantId, ClassroomShift shift) {
        return calculateClassroom(birthDate, academicYear, tenantId, shift, null);
    }
    /** Valida la sala elegida contra las opciones persistidas, sin confiar en el texto del navegador. */
    public String calculateClassroom(LocalDate birthDate, Integer academicYear, String tenantId, ClassroomShift shift, String selection) {
        Eligibility result = eligibleClassrooms(birthDate, academicYear, tenantId, shift);
        if (result.options().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.message());
        if (selection == null || selection.isBlank()) return result.options().get(0).name();
        return result.options().stream().filter(c -> c.name().equals(selection)).findFirst()
            .map(ClassroomOption::name)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La sala elegida no es válida para la edad y turno de este alumno. Actualizá la calculadora."));
    }
}
