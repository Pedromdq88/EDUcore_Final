package com.educore.sge.kindergarten.application;

import com.educore.sge.institution.infrastructure.domain.ClassroomShift;
import com.educore.sge.institution.infrastructure.entity.SchoolProfileJpaEntity;
import com.educore.sge.institution.infrastructure.repository.SchoolProfileJpaRepository;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.entity.StudentStatus;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class YearPromotionServiceTest {
    private final SchoolProfileJpaRepository profiles=mock(SchoolProfileJpaRepository.class);
    private final StudentJpaRepository students=mock(StudentJpaRepository.class);
    private final ClassroomAssignmentService classrooms=mock(ClassroomAssignmentService.class);
    private final YearPromotionService service=new YearPromotionService(profiles,students,classrooms);

    private void currentYear(int year) {
        SchoolProfileJpaEntity profile=new SchoolProfileJpaEntity();
        profile.setAcademicYear(year);
        when(profiles.findById("school")).thenReturn(Optional.of(profile));
    }
    private StudentJpaEntity student(String id,int previousYear,StudentStatus status) {
        StudentJpaEntity s=new StudentJpaEntity();
        s.setId(id);s.setTenantId("school");s.setFirstName("Alma");s.setLastName("Pérez");
        s.setAcademicYear(previousYear);s.setBirthDate(LocalDate.of(2022,5,15));
        s.setStudentShift(ClassroomShift.MANANA);s.setStatus(status);
        return s;
    }

    @Test void muestraSoloEstudiantesActivosDeAniosAnteriores() {
        currentYear(2027);
        when(students.findByTenantId("school")).thenReturn(List.of(
                student("a",2026,StudentStatus.ACTIVE),
                student("b",2027,StudentStatus.ACTIVE),
                student("c",2026,StudentStatus.WITHDRAWN),
                student("d",2026,StudentStatus.PENDING_WITHDRAWAL)));
        when(classrooms.eligibleClassrooms(any(),eq(2027),eq("school"),eq(ClassroomShift.MANANA)))
                .thenReturn(new ClassroomAssignmentService.Eligibility(4,LocalDate.of(2027,6,30),
                        List.of(new ClassroomAssignmentService.ClassroomOption("r1","Sala Roja",4,ClassroomShift.MANANA)),
                        "Una sala disponible"));
        var preview=service.preview("school",2027);
        assertEquals(1,preview.pendingCount());
        assertEquals("a",preview.students().get(0).studentId());
        verify(students,never()).save(any());
    }

    @Test void promueveSoloLaSeleccionYConservaAulasSinModificar() {
        currentYear(2027);
        var selected=student("a",2026,StudentStatus.ACTIVE);
        when(students.lockStudent("school","a")).thenReturn(Optional.of(selected));
        when(classrooms.calculateClassroom(any(),eq(2027),eq("school"),eq(ClassroomShift.MANANA),eq("Sala Roja")))
                .thenReturn("Sala Roja");
        var result=service.execute("school",new YearPromotionService.Execute(2027,
                List.of(new YearPromotionService.Selection("a","Sala Roja"))));
        assertEquals(1,result.promotedCount());
        assertEquals(2027,selected.getAcademicYear());
        assertEquals("Sala Roja",selected.getClassroom());
        verify(students).saveAll(anyList());
    }

    @Test void noPromueveAlumnosDeBajaNiDuplicados() {
        currentYear(2027);
        when(students.lockStudent("school","a"))
                .thenReturn(Optional.of(student("a",2026,StudentStatus.WITHDRAWN)));
        assertThrows(ResponseStatusException.class,()->service.execute("school",
                new YearPromotionService.Execute(2027,List.of(new YearPromotionService.Selection("a","Sala Roja")))));
        assertThrows(ResponseStatusException.class,()->service.execute("school",
                new YearPromotionService.Execute(2027,List.of(new YearPromotionService.Selection("a","Sala Roja"),
                        new YearPromotionService.Selection("a","Sala Roja")))));
        verify(students,never()).saveAll(anyList());
    }

    @Test void requiereCicloActivoAntesDePromover() {
        currentYear(2026);
        assertThrows(ResponseStatusException.class,()->service.preview("school",2027));
        verify(students,never()).findByTenantId(anyString());
    }
}
