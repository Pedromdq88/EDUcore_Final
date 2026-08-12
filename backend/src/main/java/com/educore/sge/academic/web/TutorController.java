package com.educore.sge.academic.web;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.educore.sge.academic.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.academic.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.academic.infrastructure.repository.TutorJpaRepository;
import com.educore.sge.academic.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.academic.persistence.TutorHistoryJpaEntity;
import com.educore.sge.academic.persistence.TutorHistoryRepository;
import com.educore.sge.academic.persistence.StudentHistoryJpaEntity;
import com.educore.sge.academic.persistence.StudentHistoryRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional; // 🟢 IMPORTANTE

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/tutors")
public class TutorController {

    private final TutorJpaRepository repository;
    private final StudentJpaRepository studentRepository;

    @Autowired
    private TutorHistoryRepository tutorHistoryRepository;

    @Autowired
    private StudentHistoryRepository studentHistoryRepository;

    public TutorController(TutorJpaRepository repository, StudentJpaRepository studentRepository) {
        this.repository = repository;
        this.studentRepository = studentRepository;
    }

    // LISTAR TUTORES ACTIVOS
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @GetMapping
    public List<TutorJpaEntity> getAllTutors() {
        return repository.findAll();
    }

    // REGISTRAR NUEVO TUTOR
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @PostMapping
    public TutorJpaEntity createTutor(@RequestBody TutorJpaEntity tutor) {
        tutor.setId(UUID.randomUUID().toString());
        return repository.save(tutor);
    }

    // PROCESAR BAJA HISTÓRICA DEL TUTOR CON REVISIÓN DIRECTA EN ENTORNO @ManyToMany
    @Transactional // 🟢 Asegura que todos los deletes y saves ocurran en una sola transacción atómica
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{id}/baja")
    public void darDeBajaTutor(@PathVariable String id) {
        TutorJpaEntity tutor = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor no encontrado"));

        // Buscamos todos los alumnos para analizar sus listas de tutores mapeadas
        List<StudentJpaEntity> todosLosAlumnos = studentRepository.findAll();
        List<StudentJpaEntity> alumnosAEliminar = new ArrayList<>();

        for (StudentJpaEntity alumno : todosLosAlumnos) {
            if (alumno.getTutors() != null && alumno.getTutors().contains(tutor)) {
                // Si este es su único tutor, el alumno se va de baja también
                if (alumno.getTutors().size() <= 1) {

                    // 1. Volcado completo del menor al registro histórico
                    StudentHistoryJpaEntity alumnoHistorico = new StudentHistoryJpaEntity(
                        alumno.getId(),
                        alumno.getFirstName(),
                        alumno.getLastName(),
                        alumno.getDocumentNumber(),
                        alumno.getClassroom(),
                        alumno.getBirthDate(),
                        alumno.getTelefonoContacto(),
                        alumno.getDireccion(),
                        LocalDate.now()
                    );
                    studentHistoryRepository.save(alumnoHistorico);

                    // Lo agregamos a la lista de bajas diferidas para no romper el bucle activo
                    alumnosAEliminar.add(alumno);
                } else {
                    // Si tiene más tutores (ej: padre y madre), solo removemos al que se da de baja
                    alumno.getTutors().remove(tutor);
                    studentRepository.save(alumno);
                }
            }
        }

        // 2. Ejecutamos la eliminación física en cadena de los alumnos sin tutores de forma segura
        for (StudentJpaEntity alumno : alumnosAEliminar) {
            alumno.getTutors().clear(); // Rompe los registros en la tabla intermedia student_tutors
            studentRepository.save(alumno);
            studentRepository.delete(alumno); // Borra de la tabla students
        }

        // 3. Mapeo del Tutor saliente a su correspondiente tabla histórica
        TutorHistoryJpaEntity historico = new TutorHistoryJpaEntity(
            tutor.getId(),
            tutor.getFirstName(),
            tutor.getLastName(),
            tutor.getDocumentNumber(),
            tutor.getRelationship().name(),
            tutor.getPhone(),
            tutor.getEmail(),
            LocalDate.now()
        );
        tutorHistoryRepository.save(historico);

        // 4. Desvinculamos al tutor de cualquier alumno restante antes de borrarlo definitivamente
        for (StudentJpaEntity alumno : todosLosAlumnos) {
            if (alumno.getTutors() != null) {
                alumno.getTutors().remove(tutor);
            }
        }

        // 5. Eliminación final del tutor del padrón activo
        repository.delete(tutor);
    }
}
