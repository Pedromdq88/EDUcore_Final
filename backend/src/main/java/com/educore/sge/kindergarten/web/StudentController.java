package com.educore.sge.kindergarten.web;

import com.educore.sge.kindergarten.application.ClassroomAssignmentService;
import com.educore.sge.kindergarten.application.StudentTutorService;
import com.educore.sge.kindergarten.application.dto.TutorAssignmentRequest;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.persistence.StudentHistoryJpaEntity;
import com.educore.sge.kindergarten.persistence.StudentHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentJpaRepository repository;
    private final StudentTutorService studentTutorService;
    private final StudentHistoryRepository studentHistoryRepository;
    private final ClassroomAssignmentService classroomAssignmentService;

    public StudentController(
            StudentJpaRepository repository,
            StudentTutorService studentTutorService,
            StudentHistoryRepository studentHistoryRepository,
            ClassroomAssignmentService classroomAssignmentService) {
        this.repository = repository;
        this.studentTutorService = studentTutorService;
        this.studentHistoryRepository = studentHistoryRepository;
        this.classroomAssignmentService = classroomAssignmentService;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping
    public List<StudentJpaEntity> getAllStudents(
            @RequestHeader("X-Institution-Id") String institutionId) {
        return repository.findByTenantId(institutionId);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping("/{id}")
    public StudentJpaEntity getStudentById(@PathVariable String id) {
        StudentJpaEntity student = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado con ID: " + id));

        if (student.getTutors() != null) {
            student.getTutors().size();
        }

        return student;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PostMapping
    public StudentJpaEntity createStudent(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody StudentJpaEntity student) {

        student.setId(UUID.randomUUID().toString());
        student.setTenantId(institutionId);

        if (student.getAcademicYear() == null) {
            student.setAcademicYear(LocalDate.now().getYear());
        }

        String salaCalculada = classroomAssignmentService.calculateClassroom(
                student.getBirthDate(),
                student.getAcademicYear()
        );
        student.setClassroom(salaCalculada);

        return repository.save(student);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @PostMapping("/{studentId}/tutors")
    public void linkTutors(@PathVariable String studentId, @RequestBody List<TutorAssignmentRequest> requests) {
        studentTutorService.assignTutorsToStudent(studentId, requests);
    }

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
                alumno.getContactPhone(),
                alumno.getAddress(),
                LocalDate.now()
        );
        studentHistoryRepository.save(historico);

        repository.delete(alumno);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TUTOR')")
    @PutMapping("/{id}")
    public StudentJpaEntity updateStudent(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            @RequestBody StudentJpaEntity updatedStudent) {

        StudentJpaEntity existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));

        existing.setLegajoNumber(updatedStudent.getLegajoNumber());
        existing.setFirstName(updatedStudent.getFirstName());
        existing.setLastName(updatedStudent.getLastName());
        existing.setDocumentNumber(updatedStudent.getDocumentNumber());
        existing.setBirthDate(updatedStudent.getBirthDate());

        if (updatedStudent.getAcademicYear() != null) {
            existing.setAcademicYear(updatedStudent.getAcademicYear());
        }

        String salaRecalculada = classroomAssignmentService.calculateClassroom(
                existing.getBirthDate(),
                existing.getAcademicYear()
        );
        existing.setClassroom(salaRecalculada);

        existing.setGender(updatedStudent.getGender());
        existing.setBloodType(updatedStudent.getBloodType());
        existing.setHealthInsurance(updatedStudent.getHealthInsurance());
        existing.setAllergies(updatedStudent.getAllergies());
        existing.setBirthPlace(updatedStudent.getBirthPlace());
        existing.setAddress(updatedStudent.getAddress());
        existing.setContactPhone(updatedStudent.getContactPhone());

        return repository.save(existing);
    }
    }