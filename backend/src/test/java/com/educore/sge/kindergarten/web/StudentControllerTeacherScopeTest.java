package com.educore.sge.kindergarten.web;

import com.educore.sge.auth.RoleScopeService;
import com.educore.sge.kindergarten.application.ClassroomAssignmentService;
import com.educore.sge.kindergarten.application.StudentTutorService;
import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.kindergarten.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.kindergarten.infrastructure.repository.TutorJpaRepository;
import com.educore.sge.kindergarten.persistence.StudentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentControllerTeacherScopeTest {
    private final StudentJpaRepository students = mock(StudentJpaRepository.class);
    private final RoleScopeService scopes = mock(RoleScopeService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final StudentController controller = new StudentController(students, mock(TutorJpaRepository.class),
            mock(StudentTutorService.class), mock(StudentHistoryRepository.class),
            mock(ClassroomAssignmentService.class), scopes);

    private StudentJpaEntity student(String id, String room) {
        var s = new StudentJpaEntity();
        s.setId(id); s.setTenantId("school"); s.setClassroom(room);
        s.setFirstName("Alumno"); s.setLastName("Prueba"); s.setAcademicYear(2026);
        return s;
    }

    @Test void docenteSoloListaSuAula() {
        when(scopes.is(authentication,"TEACHER")).thenReturn(true);
        when(scopes.teacherClassrooms(authentication,"school")).thenReturn(Set.of("Sala Verde"));
        when(students.findByTenantId("school")).thenReturn(List.of(
                student("s1","Sala Verde"), student("s2","Sala Azul")));
        var list=controller.getAllStudents("school",authentication);
        assertEquals(1,list.size());
    }

    @Test void docenteNoPuedeAbrirAlumnoDeOtraAulaPorId() {
        when(scopes.is(authentication,"TEACHER")).thenReturn(true);
        when(scopes.teacherClassrooms(authentication,"school")).thenReturn(Set.of("Sala Verde"));
        when(students.findById("s2")).thenReturn(Optional.of(student("s2","Sala Azul")));
        assertThrows(ResponseStatusException.class,
                () -> controller.getStudentById("s2","school",authentication));
    }
}
