package com.educore.sge.kindergarten.application;

import com.educore.sge.institution.infrastructure.repository.SchoolProfileJpaRepository;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.StudentStatus;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Promoción anual opt-in, por alumno, sin modificar tablas ni edades de las salas. */
@Service
public class YearPromotionService {
    private final SchoolProfileJpaRepository profiles;
    private final StudentJpaRepository students;
    private final ClassroomAssignmentService classrooms;

    public YearPromotionService(SchoolProfileJpaRepository profiles,
                                StudentJpaRepository students, ClassroomAssignmentService classrooms) {
        this.profiles=profiles;this.students=students;this.classrooms=classrooms;
    }
    public record Candidate(String studentId,String name,int currentYear,String currentClassroom,
            String shift, List<ClassroomAssignmentService.ClassroomOption> options,String message) {}
    public record Preview(int targetYear,int pendingCount,int withoutOptions,List<Candidate> students) {}
    public record Selection(String studentId,String classroomName) {}
    public record Execute(int targetYear,List<Selection> selections) {}
    public record Result(int promotedCount,List<String> promotedStudentIds) {}

    private void validateYear(String tenant,int year) {
        var profile=profiles.findById(tenant).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Perfil institucional no encontrado."));
        if(profile.getAcademicYear()==null || profile.getAcademicYear()!=year)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Primero guardá el ciclo lectivo activo de la institución. No se promocionó ningún alumno.");
    }
    @Transactional(readOnly=true)
    public Preview preview(String tenant,int targetYear) {
        validateYear(tenant,targetYear);
        var results=new ArrayList<Candidate>();
        for(StudentJpaEntity student:students.findByTenantId(tenant)) {
            if(student.getAcademicYear()==null || student.getAcademicYear()>=targetYear ||
                    student.getStatus()!=StudentStatus.ACTIVE)continue;
            List<ClassroomAssignmentService.ClassroomOption> opts;
            String reason;
            try {
                var eligibility=classrooms.eligibleClassrooms(student.getBirthDate(),targetYear,tenant,student.getStudentShift());
                opts=eligibility.options();reason=eligibility.message();
            }catch(ResponseStatusException ex) {
                opts=List.of();reason="No se pudieron calcular salas: "+ex.getReason();
            }
            results.add(new Candidate(student.getId(),student.getFirstName()+" "+student.getLastName(),
                    student.getAcademicYear(),student.getClassroom(),
                    student.getStudentShift()==null?"":student.getStudentShift().name(),opts,reason));
        }
        long without=results.stream().filter(c->c.options().isEmpty()).count();
        return new Preview(targetYear,results.size(),(int)without,results);
    }
    @Transactional
    public Result execute(String tenant,Execute request) {
        if(request==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selección inválida.");
        validateYear(tenant,request.targetYear());
        if(request.selections()==null || request.selections().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No seleccionaste alumnos para promocionar.");
        Set<String> seen=new HashSet<>();
        List<StudentJpaEntity> changed=new ArrayList<>();
        for(Selection choice:request.selections()) {
            if(choice==null || choice.studentId()==null || choice.classroomName()==null ||
                    !seen.add(choice.studentId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Hay alumnos repetidos o sin sala en la solicitud.");
            StudentJpaEntity student=students.lockStudent(tenant,choice.studentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Alumno no encontrado."));
            if(student.getStatus()!=StudentStatus.ACTIVE || student.getAcademicYear()==null ||
                    student.getAcademicYear()>=request.targetYear())
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El ciclo o estado de un alumno cambió. Revisá de nuevo la vista previa.");
            String room=classrooms.calculateClassroom(student.getBirthDate(),request.targetYear(),tenant,
                    student.getStudentShift(),choice.classroomName());
            student.setAcademicYear(request.targetYear());
            student.setClassroom(room);
            changed.add(student);
        }
        students.saveAll(changed);
        students.flush();
        return new Result(changed.size(),changed.stream().map(StudentJpaEntity::getId).toList());
    }
}
