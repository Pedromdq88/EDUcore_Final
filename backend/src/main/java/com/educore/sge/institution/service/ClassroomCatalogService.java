package com.educore.sge.institution.service;

import com.educore.sge.shared.domain.EducationLevel;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ClassroomCatalogService {

    private static final Map<EducationLevel, List<String>> CLASSROOMS = Map.of(
            EducationLevel.JARDIN, List.of(
                    "1° Sección (3 años)",
                    "2° Sección (4 años)",
                    "3° Sección (5 años)"
            ),
            EducationLevel.PRIMARIA, List.of(
                    "1° Grado", "2° Grado", "3° Grado",
                    "4° Grado", "5° Grado", "6° Grado"
            ),
            EducationLevel.SECUNDARIA, List.of(
                    "1° Año", "2° Año", "3° Año",
                    "4° Año", "5° Año", "6° Año"
            )
    );

    public List<String> getClassroomsForLevel(EducationLevel level) {
        if (level == null) return Collections.emptyList();
        return CLASSROOMS.getOrDefault(level, Collections.emptyList());
    }

    public String getDisplayName(EducationLevel level) {
        if (level == null) return "-";
        return switch (level) {
            case JARDIN -> "Nivel Inicial / Jardín";
            case PRIMARIA -> "Nivel Primario";
            case SECUNDARIA -> "Nivel Secundario";
        };
    }
}