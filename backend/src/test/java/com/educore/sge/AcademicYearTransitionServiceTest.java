package com.educore.sge.institution.service;

import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.StudentStatus;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcademicYearTransitionServiceTest {
    private final StudentJpaRepository repository = mock(StudentJpaRepository.class);
    private final AcademicYearTransitionService service = new AcademicYearTransitionService(
        repository, Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires")));

    private StudentJpaEntity student(String id, int year, StudentStatus status) {
        var student = new StudentJpaEntity();
        student.setId(id);
        student.setAcademicYear(year);
        student.setClassroom("Sala Verde");
        student.setStatus(status);
        return student;
    }

    @Test void dejaSinAsignarSoloAlumnosActivosAlCambiarCiclo() {
        var active = student("1", 2026, StudentStatus.ACTIVE);
        var withdrawn = student("2", 2026, StudentStatus.WITHDRAWN);
        when(repository.findByTenantId("school")).thenReturn(List.of(active, withdrawn));
        service.updateStudents("school", 2026, 2027);
        assertNull(active.getClassroom());
        assertEquals(2027, active.getAcademicYear());
        assertEquals("Sala Verde", withdrawn.getClassroom());
        verify(repository).saveAll(List.of(active));
    }

    @Test void noRepiteCambioSiEsElMismoCiclo() {
        service.updateStudents("school", 2027, 2027);
        verifyNoInteractions(repository);
    }

    @Test void rechazaRetrocederDeCiclo() {
        assertThrows(ResponseStatusException.class, () -> service.updateStudents("school", 2027, 2026));
        verifyNoInteractions(repository);
    }
}
