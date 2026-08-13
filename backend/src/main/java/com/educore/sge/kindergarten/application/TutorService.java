package com.educore.sge.kindergarten.application;

import com.educore.sge.kindergarten.infrastructure.dto.TutorDTO;
import com.educore.sge.kindergarten.infrastructure.entity.RelationshipType;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TutorService {

    private final TutorJpaRepository tutorRepository;

    public TutorService(TutorJpaRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    // GET /tutors - Obtener todos los tutores
    public List<TutorDTO> getAllTutors() {
        return tutorRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // GET /tutors/{id} - Obtener tutor por ID
    public TutorDTO getTutorById(String id) {
        TutorJpaEntity entity = tutorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado con ID: " + id));
        return toDTO(entity);
    }


    @Transactional
    public TutorDTO updateTutor(String id, TutorDTO dto) {
        TutorJpaEntity tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado con ID: " + id));

        // Actualizamos los datos básicos
        tutor.setFirstName(dto.getFirstName());
        tutor.setLastName(dto.getLastName());
        tutor.setDocumentNumber(dto.getDocumentNumber());
        tutor.setPhone(dto.getPhone());
        tutor.setEmail(dto.getEmail());
        tutor.setDireccion(dto.getDireccion());

        // Conversión de Vínculo (Enum)
        if (dto.getRelationship() != null) {
            try {
                String normalizedRelationship = dto.getRelationship().trim().replace(" ", "_").toUpperCase();
                tutor.setRelationship(RelationshipType.valueOf(normalizedRelationship));
            } catch (IllegalArgumentException e) {
                tutor.setRelationship(RelationshipType.TUTOR_LEGAL);
            }
        }

        // CAMPOS NUEVOS
        tutor.setNacionalidad(dto.getNacionalidad());
        tutor.setProfesion(dto.getProfesion());
        tutor.setCondicionActividad(dto.getCondicionActividad());
        tutor.setConvive(dto.getConvive());

        TutorJpaEntity updated = tutorRepository.save(tutor);
        return toDTO(updated);
    }

    // Mapeo Entidad JPA -> DTO
    private TutorDTO toDTO(TutorJpaEntity entity) {
        TutorDTO dto = new TutorDTO();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setDocumentNumber(entity.getDocumentNumber());

        // Conversión de Enum (JPA Entity) a String (DTO)
        if (entity.getRelationship() != null) {
            dto.setRelationship(entity.getRelationship().name());
        }

        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setDireccion(entity.getDireccion());

        // CAMPOS NUEVOS
        dto.setNacionalidad(entity.getNacionalidad());
        dto.setProfesion(entity.getProfesion());
        dto.setCondicionActividad(entity.getCondicionActividad());
        dto.setConvive(entity.getConvive());

        return dto;
    }
}