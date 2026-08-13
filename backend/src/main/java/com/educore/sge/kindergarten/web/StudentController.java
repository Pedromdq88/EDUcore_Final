package com.educore.sge.kindergarten.web;

import com.educore.sge.kindergarten.application.StudentTutorService;
import com.educore.sge.kindergarten.application.dto.TutorAssignmentRequest;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.persistence.StudentHistoryJpaEntity;
import com.educore.sge.kindergarten.persistence.StudentHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentJpaRepository repository;
    private final StudentTutorService studentTutorService;

    @Autowired
    private StudentHistoryRepository studentHistoryRepository;

    public StudentController(StudentJpaRepository repository, StudentTutorService studentTutorService) {
        this.repository = repository;
        this.studentTutorService = studentTutorService;
    }

    // GET ALL: Obtener todos los alumnos
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @GetMapping
    public List<StudentJpaEntity> getAllStudents() {
        return repository.findAll();
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @GetMapping("/{id}")
    public StudentJpaEntity getStudentById(@PathVariable String id) {
        StudentJpaEntity student = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado con ID: " + id));

        // Forzamos la carga de la colección si está configurada como LAZY en JPA
        if (student.getTutors() != null) {
            student.getTutors().size();
        }

        return student;
    }

    // POST: Crear un nuevo alumno
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @PostMapping
    public StudentJpaEntity createStudent(@RequestBody StudentJpaEntity student) {
        student.setId(UUID.randomUUID().toString());
        return repository.save(student);
    }

    // POST: Vincular tutores
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TEACHER')")
    @PostMapping("/{studentId}/tutors")
    public void linkTutors(@PathVariable String studentId, @RequestBody List<TutorAssignmentRequest> requests) {
        studentTutorService.assignTutorsToStudent(studentId, requests);
    }

    // POST: Dar de baja alumno
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{id}/baja")
    public void darDeBajaAlumno(@PathVariable String id) {
        StudentJpaEntity alumno = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));

        StudentHistoryJpaEntity historico = new StudentHistoryJpaEntity(
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
        studentHistoryRepository.save(historico);

        repository.delete(alumno);
    }

    // Permitimos que DIRECTOR, ADMINISTRATIVE y TUTOR puedan actualizar la ficha
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'TUTOR')")
    @PutMapping("/{id}")
    public StudentJpaEntity updateStudent(@PathVariable String id, @RequestBody StudentJpaEntity updatedStudent) {
        StudentJpaEntity existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));

        existing.setFirstName(updatedStudent.getFirstName());
        existing.setLastName(updatedStudent.getLastName());
        existing.setDocumentNumber(updatedStudent.getDocumentNumber());
        existing.setClassroom(updatedStudent.getClassroom());
        existing.setDireccion(updatedStudent.getDireccion());

        return repository.save(existing);
    }
}