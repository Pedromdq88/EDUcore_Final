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
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String curso) {

        // Limpiar el texto ingresado en el buscador.
        if (busqueda != null) {
            busqueda = busqueda.trim();

            if (busqueda.isEmpty()) {
                busqueda = null;
            }
        }

        // Limpiar el filtro de curso.
        if (curso != null) {
            curso = curso.trim();

            if (curso.isEmpty()) {
                curso = null;
            }
        }

        return repository.buscarAlumnos(
                institutionId,
                busqueda,
                curso
        );
    }


    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping("/{id}")
    public StudentJpaEntity getStudentById(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {

        StudentJpaEntity student = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Alumno no encontrado"
                ));

        if (!institutionId.equals(student.getTenantId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Alumno no encontrado"
            );
        }

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

        if (student.getDocumentNumber() == null
                || student.getDocumentNumber().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El DNI del alumno es obligatorio."
            );
        }

        String dni = student.getDocumentNumber().trim();

        if (repository.existsByTenantIdAndDocumentNumber(institutionId, dni)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un alumno con ese DNI en la institución."
            );
        }

        String legajo = student.getLegajoNumber();

        if (legajo != null && !legajo.isBlank()) {
            legajo = legajo.trim();

            if (repository.existsByTenantIdAndLegajoNumber(institutionId, legajo)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe un alumno con ese legajo en la institución."
                );
            }

            student.setLegajoNumber(legajo);
        }

        student.setDocumentNumber(dni);
        student.setId(UUID.randomUUID().toString());
        student.setTenantId(institutionId);

        if (student.getAcademicYear() == null) {
            student.setAcademicYear(LocalDate.now().getYear());
        }

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
    public void darDeBajaAlumno(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id) {

        StudentJpaEntity alumno = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Alumno no encontrado"
                ));

        // Verificar que el alumno pertenezca a la institución.
        if (!institutionId.equals(alumno.getTenantId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Alumno no encontrado"
            );
        }

        // Guardar el historial del alumno antes de eliminarlo.
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Alumno no encontrado"
                ));

        // Verificar que el alumno pertenezca a la institución.
        if (!institutionId.equals(existing.getTenantId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Alumno no encontrado"
            );
        }

        // Validar que el DNI sea obligatorio.
        if (updatedStudent.getDocumentNumber() == null
                || updatedStudent.getDocumentNumber().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El DNI del alumno es obligatorio."
            );
        }

        String dni = updatedStudent.getDocumentNumber().trim();

        // Comprobar que otro alumno no tenga el mismo DNI.
        if (repository.existsByTenantIdAndDocumentNumberAndIdNot(
                institutionId, dni, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe otro alumno con ese DNI en la institución."
            );
        }

        // Validar el legajo si se informó.
        String legajo = updatedStudent.getLegajoNumber();

        if (legajo != null && !legajo.isBlank()) {
            legajo = legajo.trim();

            if (repository.existsByTenantIdAndLegajoNumberAndIdNot(
                    institutionId, legajo, id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe otro alumno con ese legajo en la institución."
                );
            }
        }

        // Datos básicos.
        existing.setLegajoNumber(legajo);
        existing.setFirstName(updatedStudent.getFirstName());
        existing.setLastName(updatedStudent.getLastName());
        existing.setDocumentNumber(dni);
        existing.setBirthDate(updatedStudent.getBirthDate());

        if (updatedStudent.getAcademicYear() != null) {
            existing.setAcademicYear(updatedStudent.getAcademicYear());
        }

        // Aula manual o calculada.
        if (updatedStudent.getClassroom() != null
                && !updatedStudent.getClassroom().isBlank()) {
            existing.setClassroom(updatedStudent.getClassroom());
        } else {
            String salaRecalculada =
                    classroomAssignmentService.calculateClassroom(
                            existing.getBirthDate(),
                            existing.getAcademicYear(),
                            institutionId
                    );

            existing.setClassroom(salaRecalculada);
        }

        // Identidad y documentación legal.
        existing.setCuil(updatedStudent.getCuil());
        existing.setDniStatus(updatedStudent.getDniStatus());
        existing.setGenderIdentity(updatedStudent.getGenderIdentity());

        // Origen y nacimiento.
        existing.setBirthCountry(updatedStudent.getBirthCountry());
        existing.setNationality(updatedStudent.getNationality());
        existing.setBirthProvince(updatedStudent.getBirthProvince());
        existing.setBirthLocality(updatedStudent.getBirthLocality());

        // Domicilio estructurado.
        existing.setStreet(updatedStudent.getStreet());
        existing.setStreetNumber(updatedStudent.getStreetNumber());
        existing.setFloor(updatedStudent.getFloor());
        existing.setTower(updatedStudent.getTower());
        existing.setApartment(updatedStudent.getApartment());
        existing.setBetweenStreets(updatedStudent.getBetweenStreets());
        existing.setAddressLocality(updatedStudent.getAddressLocality());

        // Datos sociodemográficos y escolares.
        existing.setHasSiblings(updatedStudent.getHasSiblings());
        existing.setSiblingCount(updatedStudent.getSiblingCount());
        existing.setSiblingsInThisSchool(
                updatedStudent.getSiblingsInThisSchool()
        );
        existing.setReceivesAuh(updatedStudent.getReceivesAuh());
        existing.setBelongsToNativePeople(
                updatedStudent.getBelongsToNativePeople()
        );
        existing.setTransportationMethods(
                updatedStudent.getTransportationMethods()
        );

        // Contacto y salud.
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