package com.educore.sge.academic.web;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.educore.sge.academic.application.StudentTutorService;
import com.educore.sge.academic.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.academic.infrastructure.repository.StudentJpaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.educore.sge.academic.persistence.StudentHistoryJpaEntity;
import com.educore.sge.academic.persistence.StudentHistoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentJpaRepository repository;
    private final StudentTutorService studentTutorService;

    public StudentController(StudentJpaRepository repository, StudentTutorService studentTutorService) {
        this.repository = repository;
        this.studentTutorService = studentTutorService;
    }

    // 🆕 NUEVO MÉTODO GET: Resuelve el 404 del Front al cargar la tabla
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @GetMapping
    public List<StudentJpaEntity> getAllStudents() {
        return repository.findAll();
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @PostMapping
    public StudentJpaEntity createStudent(@RequestBody StudentJpaEntity student) {
        student.setId(UUID.randomUUID().toString()); // <--- Genera un ID único alfanumérico largo
        return repository.save(student);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @PostMapping("/{studentId}/tutors/{tutorId}")
    public void linkTutor(@PathVariable String studentId, @PathVariable String tutorId) {
        studentTutorService.linkTutorToStudent(studentId, tutorId);
    }

    // Asegurate de inyectar el nuevo repositorio arriba
    @Autowired
    private StudentHistoryRepository studentHistoryRepository;

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{id}/baja")
    public void darDeBajaAlumno(@PathVariable String id) {
        StudentJpaEntity alumno = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));

        // Guardamos en la tabla histórica con la fecha del click
        StudentHistoryJpaEntity historico = new StudentHistoryJpaEntity(
            alumno.getId(),
            alumno.getFirstName(),
            alumno.getLastName(),
            alumno.getDocumentNumber(),
            alumno.getClassroom(),
            alumno.getBirthDate(),
            alumno.getTelefonoContacto(),
            alumno.getDireccion(),
            LocalDate.now() // <--- Fecha exacta del momento
        );
        studentHistoryRepository.save(historico);

        // Lo removemos de la escuela activa
        repository.delete(alumno);
    }
    }
