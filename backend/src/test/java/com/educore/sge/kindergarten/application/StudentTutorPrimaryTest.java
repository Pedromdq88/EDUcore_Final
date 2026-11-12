package com.educore.sge.kindergarten.application;

import com.educore.sge.kindergarten.application.dto.TutorAssignmentRequest;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentTutorPrimaryTest {
    private final StudentJpaRepository students = mock(StudentJpaRepository.class);
    private final TutorJpaRepository tutors = mock(TutorJpaRepository.class);
    private final StudentTutorService service = new StudentTutorService(students, tutors);

    private TutorJpaEntity tutor(String id) {
        var t = new TutorJpaEntity();t.setId(id);t.setTenantId("school");return t;
    }
    private TutorAssignmentRequest link(String id, boolean primary) {
        var r=new TutorAssignmentRequest();r.setTutorId(id);r.setIsPrimary(primary);return r;
    }

    @Test void secundarioPasaASerPrincipalAlEliminarTitular() {
        var secondary=tutor("t2");
        when(students.findPrimaryTutorId("s1")).thenReturn(Optional.empty());
        when(students.existsByIdAndTutorsId("s1", "t2")).thenReturn(true);
        when(students.markPrimaryTutor("s1","t2")).thenReturn(1);
        service.ensurePrimaryAfterRemoval("s1", "t1", List.of(secondary));
        verify(students).clearPrimaryTutor("s1");
        verify(students).markPrimaryTutor("s1", "t2");
    }

    @Test void siSeEliminaElSecundarioElPrincipalNoCambia() {
        var first=tutor("t1");
        when(students.findPrimaryTutorId("s1")).thenReturn(Optional.of("t1"));
        when(students.existsByIdAndTutorsId("s1","t1")).thenReturn(true);
        when(students.markPrimaryTutor("s1","t1")).thenReturn(1);
        service.ensurePrimaryAfterRemoval("s1", "t2", List.of(first));
        verify(students).markPrimaryTutor("s1","t1");
        verify(students,never()).markPrimaryTutor(eq("s1"),eq("t2"));
    }

    @Test void noPermiteDosPrincipales() {
        var student = new StudentJpaEntity();student.setId("s1");student.setTenantId("school");
        when(students.findById("s1")).thenReturn(Optional.of(student));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                service.assignTutorsToStudent("s1",List.of(link("t1",true),link("t2",true))));
        verify(students,never()).saveAndFlush(any());
    }

    @Test void edicionActualizaUnicoPrincipal() {
        var student=new StudentJpaEntity();student.setId("s1");student.setTenantId("school");
        var one=tutor("t1");var two=tutor("t2");
        when(students.findById("s1")).thenReturn(Optional.of(student));
        when(students.lockStudent("school", "s1")).thenReturn(Optional.of(student));
        when(tutors.findById("t1")).thenReturn(Optional.of(one));
        when(tutors.findById("t2")).thenReturn(Optional.of(two));
        when(students.existsByIdAndTutorsId("s1","t2")).thenReturn(true);
        when(students.markPrimaryTutor("s1","t2")).thenReturn(1);
        service.assignTutorsToStudent("s1",List.of(link("t1",false),link("t2",true)));
        verify(students).saveAndFlush(student);
        verify(students).markPrimaryTutor("s1","t2");
    }
}
