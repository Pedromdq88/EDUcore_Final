package com.educore.sge.kindergarten.web;

import com.educore.sge.auth.AuthService;
import com.educore.sge.auth.RoleScopeService;
import com.educore.sge.kindergarten.application.TutorService;
import com.educore.sge.kindergarten.application.StudentTutorService;
import com.educore.sge.kindergarten.infrastructure.entity.RelationshipType;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import com.educore.sge.kindergarten.persistence.StudentHistoryJpaEntity;
import com.educore.sge.kindergarten.persistence.StudentHistoryRepository;
import com.educore.sge.kindergarten.persistence.TutorHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TutorBajaConAlumnosTest {
    private final TutorJpaRepository tutors = mock(TutorJpaRepository.class);
    private final StudentJpaRepository students = mock(StudentJpaRepository.class);
    private final StudentHistoryRepository history = mock(StudentHistoryRepository.class);
    private final TutorHistoryRepository tutorHistory = mock(TutorHistoryRepository.class);
    private final AuthService auth = mock(AuthService.class);
    private final StudentTutorService primary = mock(StudentTutorService.class);
    private TutorController controller;

    @BeforeEach
    void setUp() {
        controller = new TutorController(tutors, students, mock(TutorService.class), auth, mock(RoleScopeService.class));
        ReflectionTestUtils.setField(controller, "studentHistoryRepository", history);
        ReflectionTestUtils.setField(controller, "tutorHistoryRepository", tutorHistory);
        ReflectionTestUtils.setField(controller, "studentTutorService", primary);
    }

    private TutorJpaEntity tutor(String id) {
        TutorJpaEntity tutor = new TutorJpaEntity();
        tutor.setId(id);
        tutor.setTenantId("school");
        tutor.setFirstName("Tutor");
        tutor.setLastName(id);
        tutor.setDocumentNumber("12345678");
        tutor.setRelationship(RelationshipType.PADRE);
        return tutor;
    }

    private StudentJpaEntity student(String id, TutorJpaEntity... tutors) {
        StudentJpaEntity student = new StudentJpaEntity();
        student.setId(id);
        student.setTenantId("school");
        student.setFirstName("Alumno");
        student.setLastName(id);
        student.setDocumentNumber("23456789");
        student.setBirthDate(LocalDate.of(2021, 1, 10));
        student.setTutors(new ArrayList<>(List.of(tutors)));
        return student;
    }

    @Test
    void ultimoTutorEliminaAlumnoYGuardaHistorial() {
        var tutor = tutor("t1");
        var alumno = student("s1", tutor);
        when(tutors.findById("t1")).thenReturn(Optional.of(tutor));
        when(students.findByTenantIdAndTutor("school", "t1")).thenReturn(List.of(alumno));
        when(students.lockStudent("school", "s1")).thenReturn(Optional.of(alumno));

        controller.darDeBajaTutor("school", "t1");

        verify(history).save(any(StudentHistoryJpaEntity.class));
        verify(students).delete(alumno);
        verify(auth).disableTutorAccount("school", "t1");
        verify(tutors).delete(tutor);
    }

    @Test
    void conservaAlumnoQueTieneOtroTutor() {
        var tutor = tutor("t1");
        var restante = tutor("t2");
        var alumno = student("s1", tutor, restante);
        when(tutors.findById("t1")).thenReturn(Optional.of(tutor));
        when(students.findByTenantIdAndTutor("school", "t1")).thenReturn(List.of(alumno));
        when(students.lockStudent("school", "s1")).thenReturn(Optional.of(alumno));

        controller.darDeBajaTutor("school", "t1");

        assertEquals(List.of(restante), alumno.getTutors());
        verify(students).saveAndFlush(alumno);
        verify(primary).ensurePrimaryAfterRemoval("s1", "t1", List.of(restante));
        verify(students, never()).delete(any());
        verify(history, never()).save(any());
        verify(tutors).delete(tutor);
    }

    @Test
    void vistaPreviaIndicaLosAlumnosQueSeEliminaran() {
        var tutor = tutor("t1");
        when(tutors.findById("t1")).thenReturn(Optional.of(tutor));
        when(students.findByTenantIdAndTutor("school", "t1"))
                .thenReturn(List.of(student("s1", tutor), student("s2", tutor, tutor("t2"))));

        TutorController.BajaPreview preview = controller.vistaPreviaBajaTutor("school", "t1");
        assertEquals(1, preview.alumnosQueSeDaranDeBaja());
        assertEquals(1, preview.alumnosQueConservanTutor());
        assertEquals(List.of("Alumno s1"), preview.nombresAlumnosQueSeDaranDeBaja());
    }
}
