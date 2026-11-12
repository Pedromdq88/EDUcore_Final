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
import com.educore.sge.kindergarten.persistence.StudentHistoryRepository;
import com.educore.sge.kindergarten.persistence.TutorHistoryRepository;
import com.educore.sge.kindergarten.persistence.StudentHistoryJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TutorBajaSinResponsablesTest {
    @Test
    void ultimoTutorDaDeBajaAlumnoEnMismaOperacion() {
        var tutors = mock(TutorJpaRepository.class);
        var students = mock(StudentJpaRepository.class);
        var studentHistory = mock(StudentHistoryRepository.class);
        var tutorHistory = mock(TutorHistoryRepository.class);
        var auth = mock(AuthService.class);
        var controller = new TutorController(tutors, students, mock(TutorService.class),
                auth, mock(RoleScopeService.class));
        ReflectionTestUtils.setField(controller, "tutorHistoryRepository", tutorHistory);
        ReflectionTestUtils.setField(controller, "studentHistoryRepository", studentHistory);
        ReflectionTestUtils.setField(controller, "studentTutorService", mock(StudentTutorService.class));

        var guardian = new TutorJpaEntity();
        guardian.setId("t1");
        guardian.setTenantId("school");
        guardian.setRelationship(RelationshipType.MADRE);
        guardian.setFirstName("Tutor");
        guardian.setLastName("A");
        guardian.setDocumentNumber("12345678");

        var pupil = new StudentJpaEntity();
        pupil.setId("s1");
        pupil.setTenantId("school");
        pupil.setFirstName("Alumno");
        pupil.setLastName("A");
        pupil.setBirthDate(LocalDate.of(2020, 3, 12));
        pupil.setDocumentNumber("23456789");
        pupil.setTutors(new ArrayList<>(List.of(guardian)));

        when(tutors.findById("t1")).thenReturn(Optional.of(guardian));
        when(students.findByTenantIdAndTutor("school", "t1")).thenReturn(List.of(pupil));
        when(students.lockStudent("school", "s1")).thenReturn(Optional.of(pupil));

        controller.darDeBajaTutor("school", "t1");
        verify(studentHistory).save(any(StudentHistoryJpaEntity.class));
        verify(students).delete(pupil);
        verify(tutors).delete(guardian);
        verify(auth).disableTutorAccount("school", "t1");
    }
}
