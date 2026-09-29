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

        // 🟢 Se pasa institutionId para que el servicio busque las aulas configuradas de esta escuela
        String salaCalculada = classroomAssignmentService.calculateClassroom(
                student.getBirthDate(),
                student.getAcademicYear(),
                institutionId
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

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PutMapping("/{id}")
    public StudentJpaEntity updateStudent(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            @RequestBody StudentJpaEntity updatedStudent) {

        StudentJpaEntity existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));

        // Datos básicos
        existing.setLegajoNumber(updatedStudent.getLegajoNumber());
        existing.setFirstName(updatedStudent.getFirstName());
        existing.setLastName(updatedStudent.getLastName());
        existing.setDocumentNumber(updatedStudent.getDocumentNumber());
        existing.setBirthDate(updatedStudent.getBirthDate());

        if (updatedStudent.getAcademicYear() != null) {
            existing.setAcademicYear(updatedStudent.getAcademicYear());
        }

        // 🟢 Aula manual o automática
        if (updatedStudent.getClassroom() != null && !updatedStudent.getClassroom().isBlank()) {
            existing.setClassroom(updatedStudent.getClassroom());
        } else {
            // 🟢 Se pasa institutionId (o existing.getTenantId()) para recalcular según las reglas del gestor
            String salaRecalculada = classroomAssignmentService.calculateClassroom(
                    existing.getBirthDate(),
                    existing.getAcademicYear(),
                    institutionId
            );
            existing.setClassroom(salaRecalculada);
        }

        // 1. Identidad y Documentación Legal
        existing.setCuil(updatedStudent.getCuil());
        existing.setDniStatus(updatedStudent.getDniStatus());
        existing.setGenderIdentity(updatedStudent.getGenderIdentity());

        // 2. Origen y Nacimiento
        existing.setBirthCountry(updatedStudent.getBirthCountry());
        existing.setNationality(updatedStudent.getNationality());
        existing.setBirthProvince(updatedStudent.getBirthProvince());
        existing.setBirthLocality(updatedStudent.getBirthLocality());

        // 3. Domicilio Estructurado
        existing.setStreet(updatedStudent.getStreet());
        existing.setStreetNumber(updatedStudent.getStreetNumber());
        existing.setFloor(updatedStudent.getFloor());
        existing.setTower(updatedStudent.getTower());
        existing.setApartment(updatedStudent.getApartment());
        existing.setBetweenStreets(updatedStudent.getBetweenStreets());
        existing.setAddressLocality(updatedStudent.getAddressLocality());

        // 4. Datos Sociodemográficos y Escolares
        existing.setHasSiblings(updatedStudent.getHasSiblings());
        existing.setSiblingCount(updatedStudent.getSiblingCount());
        existing.setSiblingsInThisSchool(updatedStudent.getSiblingsInThisSchool());
        existing.setReceivesAuh(updatedStudent.getReceivesAuh());
        existing.setBelongsToNativePeople(updatedStudent.getBelongsToNativePeople());
        existing.setTransportationMethods(updatedStudent.getTransportationMethods());

        // Campos anteriores de contacto y salud
        existing.setGender(updatedStudent.getGender());
        existing.setBloodType(updatedStudent.getBloodType());
        existing.setHealthInsurance(updatedStudent.getHealthInsurance());
        existing.setAllergies(updatedStudent.getAllergies());
        existing.setBirthPlace(updatedStudent.getBirthPlace());
        existing.setAddress(updatedStudent.getAddress());
        existing.setContactPhone(updatedStudent.getContactPhone());
        existing.setStudentShift(updatedStudent.getStudentShift());

        return repository.save(existing);
    }
}