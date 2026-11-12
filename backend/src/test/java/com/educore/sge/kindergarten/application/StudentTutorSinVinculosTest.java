package com.educore.sge.kindergarten.application;

import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StudentTutorSinVinculosTest {
    @Test
    void listaVaciaNoEstaPermitidaParaUnAlumno() {
        var students = mock(StudentJpaRepository.class);
        var service = new StudentTutorService(students, mock(TutorJpaRepository.class));
        var student = new StudentJpaEntity();
        student.setId("s1");
        student.setTutors(new ArrayList<>());
        when(students.findById("s1")).thenReturn(Optional.of(student));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.assignTutorsToStudent("s1", List.of()));
        verify(students, never()).save(any());
        verify(students, never()).delete(any());
    }
}
