package com.educore.sge.kindergarten.web;

import com.educore.sge.auth.RoleScopeService;
import com.educore.sge.kindergarten.application.ClassroomAssignmentService;
import com.educore.sge.kindergarten.application.StudentTutorService;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import com.educore.sge.kindergarten.persistence.StudentHistoryRepository;
import com.educore.sge.institution.infrastructure.domain.ClassroomShift;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StudentAltaTutorPrincipalTest {
    private final StudentJpaRepository students = mock(StudentJpaRepository.class);
    private final TutorJpaRepository tutors = mock(TutorJpaRepository.class);
    private final StudentTutorService links = mock(StudentTutorService.class);
    private final ClassroomAssignmentService classrooms = mock(ClassroomAssignmentService.class);
    private final StudentController controller = new StudentController(students, tutors, links,
            mock(StudentHistoryRepository.class), classrooms, mock(RoleScopeService.class));

    private StudentJpaEntity sample(String... tutorIds) {
        var student = new StudentJpaEntity();
        student.setFirstName("Ana");
        student.setLastName("Pérez");
        student.setBirthDate(LocalDate.of(2022, 1, 1));
        student.setAcademicYear(2026);
        student.setDocumentNumber("12345678");
        student.setStudentShift(ClassroomShift.MANANA);
        student.setClassroom("Sala Verde");
        student.setTutors(new ArrayList<>());
        for (String id : tutorIds) {
            var tutor = new TutorJpaEntity();
            tutor.setId(id);
            student.getTutors().add(tutor);
        }
        return student;
    }

    @Test void altaNoPermiteAlumnoSinTutor() {
        assertThrows(ResponseStatusException.class, () -> controller.createStudent("school", sample()));
        verify(students, never()).saveAndFlush(any());
    }

    @Test void altaMarcaPrimerTutorPrincipalEnLaMismaTransaccion() {
        var first = new TutorJpaEntity(); first.setId("t1"); first.setTenantId("school");
        var second = new TutorJpaEntity(); second.setId("t2"); second.setTenantId("school");
        when(tutors.findById("t1")).thenReturn(Optional.of(first));
        when(tutors.findById("t2")).thenReturn(Optional.of(second));
        when(classrooms.calculateClassroom(any(), eq(2026), eq("school"), eq(ClassroomShift.MANANA), eq("Sala Verde")))
                .thenReturn("Sala Verde");
        when(students.saveAndFlush(any())).thenAnswer(a -> a.getArgument(0));
        controller.createStudent("school", sample("t1", "t2"));
        verify(links).setPrimaryTutor(anyString(), eq("t1"));
    }
}
