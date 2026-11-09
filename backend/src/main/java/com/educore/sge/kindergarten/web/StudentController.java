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
    private final com.educore.sge.auth.RoleScopeService scopes;

    public StudentController(
            StudentJpaRepository repository,
            StudentTutorService studentTutorService,
            StudentHistoryRepository studentHistoryRepository,
            ClassroomAssignmentService classroomAssignmentService, com.educore.sge.auth.RoleScopeService scopes) {
        this.repository = repository;
        this.studentTutorService = studentTutorService;
        this.studentHistoryRepository = studentHistoryRepository;
        this.classroomAssignmentService = classroomAssignmentService;
        this.scopes=scopes;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping
    public List<?> getAllStudents(
            @RequestHeader("X-Institution-Id") String institutionId,
            org.springframework.security.core.Authentication authentication) {
        var list=repository.findByTenantId(institutionId);
        if(scopes.is(authentication,"TEACHER")) {
            var rooms=scopes.teacherClassrooms(authentication,institutionId);
            return list.stream().filter(s->rooms.contains(s.getClassroom()))
                .map(s->new StudentSummary(s.getId(),s.getFirstName(),s.getLastName(),s.getClassroom(),s.getAcademicYear())).toList();
        }
        return list;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Object getStudentById(@PathVariable String id, @RequestHeader("X-Institution-Id") String tenantId, org.springframework.security.core.Authentication authentication) {
        StudentJpaEntity student = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado con ID: " + id));

        if (!tenantId.equals(student.getTenantId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado");
        if(scopes.is(authentication,"TEACHER")) {
            if(!scopes.teacherClassrooms(authentication,tenantId).contains(student.getClassroom()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Alumno fuera de tus aulas asignadas");
            return new StudentSummary(student.getId(),student.getFirstName(),student.getLastName(),student.getClassroom(),student.getAcademicYear());
        }
        if (student.getTutors() != null) {
            student.getTutors().size();
        }

        return student;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping("/classroom-options")
    public ClassroomAssignmentService.Eligibility classroomOptions(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate birthDate,
            @RequestParam Integer academicYear,
            @RequestParam(required = false) com.educore.sge.institution.infrastructure.domain.ClassroomShift shift) {
        return classroomAssignmentService.eligibleClassrooms(birthDate, academicYear, institutionId, shift);
    }

    public record StudentSummary(String id, String firstName, String lastName, String classroom, Integer academicYear) {}

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping
    public StudentJpaEntity createStudent(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody StudentJpaEntity student) {

        student.setId(UUID.randomUUID().toString());
        student.setTenantId(institutionId);

        if (student.getAcademicYear() == null) {
            student.setAcademicYear(classroomAssignmentService.eligibleClassrooms(
                student.getBirthDate(), null, institutionId, student.getStudentShift()).cutoffDate().getYear());
        }

        // 🟢 Se pasa institutionId para que el servicio busque las aulas configuradas de esta escuela
        String salaCalculada = classroomAssignmentService.calculateClassroom(
                student.getBirthDate(),
                student.getAcademicYear(),
                institutionId, student.getStudentShift(), student.getClassroom()
        );
        student.setClassroom(salaCalculada);

        return repository.save(student);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{studentId}/tutors")
    public void linkTutors(@PathVariable String studentId,
                           @RequestHeader("X-Institution-Id") String institutionId,
                           @RequestBody List<TutorAssignmentRequest> requests) {
        StudentJpaEntity student = repository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
        if (!institutionId.equals(student.getTenantId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado");
        studentTutorService.assignTutorsToStudent(studentId, requests);
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping("/{id}/baja")
    public void darDeBajaAlumno(@PathVariable String id, @RequestHeader("X-Institution-Id") String institutionId) {
        StudentJpaEntity alumno = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));

        if (!institutionId.equals(alumno.getTenantId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado");
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

        if (!institutionId.equals(existing.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este alumno pertenece a otra institución.");
        }
        // Datos básicos
        existing.setLegajoNumber(updatedStudent.getLegajoNumber());
        existing.setFirstName(updatedStudent.getFirstName());
        existing.setLastName(updatedStudent.getLastName());
        existing.setDocumentNumber(updatedStudent.getDocumentNumber());
        existing.setBirthDate(updatedStudent.getBirthDate());
        if (updatedStudent.getStudentShift() != null) existing.setStudentShift(updatedStudent.getStudentShift());

        if (updatedStudent.getAcademicYear() != null) {
            existing.setAcademicYear(updatedStudent.getAcademicYear());
        }

        // Valida siempre la sala elegida contra la calculadora de la institución.
        existing.setClassroom(classroomAssignmentService.calculateClassroom(
                existing.getBirthDate(), existing.getAcademicYear(), institutionId,
                existing.getStudentShift(), updatedStudent.getClassroom()));

        // 1. Identidad y Documentación Legal
        existing.setCuil(updatedStudent.getCuil());
        existing.setDniStatus(updatedStudent.getDniStatus());
        existing.setHasCpi(updatedStudent.getHasCpi());
        existing.setHasForeignDocument(updatedStudent.getHasForeignDocument());
        existing.setForeignDocumentType(updatedStudent.getForeignDocumentType());
        existing.setForeignDocumentNumber(updatedStudent.getForeignDocumentNumber());
        existing.setGenderIdentity(updatedStudent.getGenderIdentity());

        // 2. Origen y Nacimiento
        existing.setBirthCountry(updatedStudent.getBirthCountry());
        existing.setNationality(updatedStudent.getNationality());
        existing.setBirthProvince(updatedStudent.getBirthProvince());
        existing.setBirthDistrict(updatedStudent.getBirthDistrict());
        existing.setBirthLocality(updatedStudent.getBirthLocality());

        // 3. Domicilio Estructurado
        existing.setStreet(updatedStudent.getStreet());
        existing.setStreetNumber(updatedStudent.getStreetNumber());
        existing.setFloor(updatedStudent.getFloor());
        existing.setTower(updatedStudent.getTower());
        existing.setApartment(updatedStudent.getApartment());
        existing.setBetweenStreets(updatedStudent.getBetweenStreets());
        existing.setOtherAddressDetails(updatedStudent.getOtherAddressDetails());
        existing.setAddressDistrict(updatedStudent.getAddressDistrict());
        existing.setStudentLandlinePhone(updatedStudent.getStudentLandlinePhone());
        existing.setStudentCellphone(updatedStudent.getStudentCellphone());
        existing.setAddressLocality(updatedStudent.getAddressLocality());

        // 4. Datos Sociodemográficos y Escolares
        existing.setHasSiblings(updatedStudent.getHasSiblings());
        existing.setSpeaksOtherLanguageAtHome(updatedStudent.getSpeaksOtherLanguageAtHome());
        existing.setIndigenousLanguage(updatedStudent.getIndigenousLanguage());
        existing.setOtherLanguage(updatedStudent.getOtherLanguage());
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