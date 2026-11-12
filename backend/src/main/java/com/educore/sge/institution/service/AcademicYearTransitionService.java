package com.educore.sge.institution.service;

import com.educore.sge.kindergarten.infrastructure.entity.StudentStatus;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AcademicYearTransitionService {
    private final StudentJpaRepository students;
    private final Clock clock;

    @Autowired
    public AcademicYearTransitionService(StudentJpaRepository students) {
        this(students, Clock.system(ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    AcademicYearTransitionService(StudentJpaRepository students, Clock clock) {
        this.students = students;
        this.clock = clock;
    }

    public void updateStudents(String institutionId, Integer previousYear, Integer newYear) {
        AcademicYearTransitionPolicy.validate(previousYear, newYear, LocalDate.now(clock));
        if (previousYear == null || previousYear.equals(newYear)) return;

        var changed = new ArrayList<StudentJpaEntity>();
        for (var student : students.findByTenantId(institutionId)) {
            if (student.getStatus() != StudentStatus.ACTIVE) continue;
            if (student.getAcademicYear() != null && student.getAcademicYear() > previousYear) continue;
            student.setAcademicYear(newYear);
            student.setClassroom(null);
            changed.add(student);
        }
        if (!changed.isEmpty()) {
            students.saveAll(changed);
            students.flush();
        }
    }
}
