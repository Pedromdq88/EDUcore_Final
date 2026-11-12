package com.educore.sge.kindergarten.web;

import com.educore.sge.kindergarten.application.ClassroomAssignmentService;
import com.educore.sge.institution.infrastructure.domain.ClassroomShift;
import com.educore.sge.kindergarten.application.StudentTutorService;
import com.educore.sge.kindergarten.application.dto.TutorAssignmentRequest;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.kindergarten.persistence.StudentHistoryJpaEntity;
import com.educore.sge.kindergarten.persistence.StudentHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentJpaRepository repository;
    private final TutorJpaRepository tutorRepository;
    private final StudentTutorService studentTutorService;
    private final StudentHistoryRepository studentHistoryRepository;
    private final ClassroomAssignmentService classroomAssignmentService;
    private final com.educore.sge.auth.RoleScopeService scopes;

    public StudentController(
            StudentJpaRepository repository,
            TutorJpaRepository tutorRepository,
            StudentTutorService studentTutorService,
            StudentHistoryRepository studentHistoryRepository,
            ClassroomAssignmentService classroomAssignmentService, com.educore.sge.auth.RoleScopeService scopes) {
        this.repository = repository;
        this.tutorRepository = tutorRepository;
        this.studentTutorService = studentTutorService;
        this.studentHistoryRepository = studentHistoryRepository;
        this.classroomAssignmentService = classroomAssignmentService;
        this.scopes=scopes;
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER')")
    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<?> getAllStudents(
            @RequestHeader("X-Institution-Id") String institutionId,
            org.springframework.security.core.Authentication authentication) {
        var list=repository.findByTenantId(institutionId);
        if(scopes.is(authentication,"TEACHER")) {
            var rooms=scopes.teacherClassrooms(authentication,institutionId);
            return list.stream().filter(s->rooms.contains(s.getClassroom()))
                .map(s->new StudentSummary(s.getId(),s.getFirstName(),s.getLastName(),s.getClassroom(),s.getAcademicYear())).toList();
        }
        // La planilla de Dirección necesita ambos contactos; inicializar la lista
        // dentro de la transacción para no depender de Open Session in View.
        for (var student : list) {
            if (student.getTutors() != null) student.getTutors().size();
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

    private void validateStudentIdentityAndContact(StudentJpaEntity student) {
        // Los registros con documentación física deben llevar únicamente dígitos.
        // La condición "sin DNI" / "en trámite" se maneja sin inventar números.
        if (student.getDniStatus() == null ||
                "FISICO".equals(student.getDniStatus().name())) {
            student.setDocumentNumber(com.educore.sge.shared.validation.RecordValidation
                    .requiredDni(student.getDocumentNumber(), "alumno"));
        } else if (student.getDocumentNumber() != null &&
                student.getDocumentNumber().matches("[0-9]+")) {
            student.setDocumentNumber(com.educore.sge.shared.validation.RecordValidation
                    .requiredDni(student.getDocumentNumber(), "alumno"));
        }
        student.setStudentLandlinePhone(com.educore.sge.shared.validation.RecordValidation
                .optionalPhone(student.getStudentLandlinePhone(), "contacto del alumno"));
        student.setStudentCellphone(com.educore.sge.shared.validation.RecordValidation
                .optionalPhone(student.getStudentCellphone(), "celular del alumno"));
        student.setContactPhone(com.educore.sge.shared.validation.RecordValidation
                .optionalPhone(student.getContactPhone(), "contacto del alumno"));
    }

    private void validateStudentDuplicates(String institutionId, String currentId, StudentJpaEntity student) {
        String dni = com.educore.sge.shared.validation.RecordValidation.comparableDni(student.getDocumentNumber());
        String legajo = student.getLegajoNumber() == null ? "" : student.getLegajoNumber().trim();
        for (StudentJpaEntity existing : repository.findByTenantId(institutionId)) {
            if (existing.getId().equals(currentId)) continue;
            if (!dni.isEmpty() && dni.equals(
                com.educore.sge.shared.validation.RecordValidation.comparableDni(existing.getDocumentNumber()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un alumno con ese DNI en esta institución.");
            }
            if (!legajo.isEmpty() && legajo.equalsIgnoreCase(
                existing.getLegajoNumber() == null ? "" : existing.getLegajoNumber().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un alumno con ese número de legajo en esta institución.");
            }
        }
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public StudentJpaEntity createStudent(
            @RequestHeader("X-Institution-Id") String institutionId,
            @RequestBody StudentJpaEntity student) {

        if (student.getFirstName() == null || student.getFirstName().isBlank() ||
            student.getLastName() == null || student.getLastName().isBlank() || student.getBirthDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre, apellido y fecha de nacimiento son obligatorios.");
        }
        // Alta atómica: no se guarda un alumno que todavía no tenga tutor.
        List<TutorJpaEntity> tutorInputs = student.getTutors();
        if (tutorInputs == null || tutorInputs.isEmpty() || tutorInputs.size() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccioná al menos un tutor (máximo dos).");
        }
        List<TutorJpaEntity> tutores = new java.util.ArrayList<>();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (TutorJpaEntity inputTutor : tutorInputs) {
            if (inputTutor == null || inputTutor.getId() == null || !ids.add(inputTutor.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pueden repetir tutores ni enviar un tutor vacío.");
            }
            TutorJpaEntity tutor = tutorRepository.findById(inputTutor.getId())
                    .filter(t -> institutionId.equals(t.getTenantId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El tutor seleccionado no existe en esta institución."));
            tutores.add(tutor);
        }
        student.setTutors(tutores);
        validateStudentIdentityAndContact(student);
        validateStudentDuplicates(institutionId, null, student);
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

        StudentJpaEntity saved = repository.saveAndFlush(student);
        // Primer tutor elegido por el usuario es principal desde el alta atómica.
        studentTutorService.setPrimaryTutor(saved.getId(), tutores.get(0).getId());
        return saved;
    }

    @GetMapping("/{studentId}/tutors/primary")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Map<String, String> primaryTutor(
            @PathVariable String studentId, @RequestHeader("X-Institution-Id") String tenantId) {
        StudentJpaEntity student = repository.findById(studentId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
        // Para matrículas históricas sin marca, el usuario debe confirmar el principal
        // en el formulario; no inventar un responsable por orden alfabético.
        String id = repository.findPrimaryTutorId(studentId).orElse("");
        return java.util.Map.of("tutorId", id);
    }

    @GetMapping("/tutor-primaries")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE','PRECEPTOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Map<String, String> primaryTutors(@RequestHeader("X-Institution-Id") String tenantId) {
        java.util.Map<String, String> values = new java.util.HashMap<>();
        repository.findPrimaryTutorsByTenant(tenantId).forEach(row -> {
            values.put(String.valueOf(row[0]), String.valueOf(row[1]));
        });
        return values;
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
    @org.springframework.transaction.annotation.Transactional
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

    public record ClassroomChange(String classroomName, ClassroomShift shift) {}

    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMINISTRATIVE')")
    @PatchMapping("/{id}/classroom")
    @org.springframework.transaction.annotation.Transactional
    public StudentJpaEntity assignClassroom(
            @RequestHeader("X-Institution-Id") String institutionId,
            @PathVariable String id,
            @RequestBody ClassroomChange request) {
        StudentJpaEntity student = repository.lockStudent(institutionId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado."));
        if (student.getStatus() != com.educore.sge.kindergarten.infrastructure.entity.StudentStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El alumno no está activo.");
        if (student.getAcademicYear() == null || student.getAcademicYear() != classroomAssignmentService.currentYear(institutionId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El alumno no pertenece al ciclo lectivo activo.");
        if (request == null || request.shift() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí un turno y un aula.");
        student.setClassroom(classroomAssignmentService.selectExistingClassroom(
                institutionId, request.shift(), request.classroomName()));
        student.setStudentShift(request.shift());
        return repository.save(student);
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
        // Revalidar duplicados también al editar, exceptuando el mismo registro.
        validateStudentIdentityAndContact(updatedStudent);
        validateStudentDuplicates(institutionId, id, updatedStudent);
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

        ClassroomShift selectedShift = updatedStudent.getStudentShift() != null
                ? updatedStudent.getStudentShift() : existing.getStudentShift();
        existing.setClassroom(classroomAssignmentService.selectExistingClassroom(
                institutionId, selectedShift, updatedStudent.getClassroom()));
        existing.setStudentShift(selectedShift);

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
        return repository.save(existing);
    }
}