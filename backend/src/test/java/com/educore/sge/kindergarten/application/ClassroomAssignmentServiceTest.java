
package com.educore.sge.kindergarten.application;

import com.educore.sge.institution.infrastructure.entity.ClassroomConfigJpaEntity;
import com.educore.sge.institution.infrastructure.entity.SchoolProfileJpaEntity;
import com.educore.sge.institution.infrastructure.domain.ClassroomShift;
import com.educore.sge.institution.infrastructure.repository.ClassroomConfigRepository;
import com.educore.sge.institution.infrastructure.repository.SchoolProfileJpaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassroomAssignmentServiceTest {

  private ClassroomConfigJpaEntity room(String id, String name, int age) {
    var r = new ClassroomConfigJpaEntity();
    r.setId(id);
    r.setName(name);
    r.setMinimumAge(age);
    r.setShift(ClassroomShift.MANANA);
    return r;
  }

  // Configura el ciclo lectivo 2026 para los tests
  private ClassroomAssignmentService createService(
          ClassroomConfigRepository repo) {

    var profiles = mock(SchoolProfileJpaRepository.class);
    var profile = mock(SchoolProfileJpaEntity.class);

    when(profiles.findById("school"))
            .thenReturn(Optional.of(profile));

    when(profile.getAcademicYear()).thenReturn(2026);

    return new ClassroomAssignmentService(repo, profiles);
  }

  @Test
  void offersAllDatabaseRoomsForCalculatedAgeAndRespectsChoice() {

    var repo = mock(ClassroomConfigRepository.class);

    when(repo.findByTenantId("school")).thenReturn(
            List.of(
                    room("1", "Sala Verde", 3),
                    room("2", "Sala Azul", 3),
                    room("3", "Sala Roja", 4)
            )
    );

    var service = createService(repo);

    var opts = service.eligibleClassrooms(
            LocalDate.of(2023, 6, 30),
            2026,
            "school",
            ClassroomShift.MANANA
    );

    assertEquals(3, opts.ageAtCutoff());
    assertEquals(2, opts.options().size());

    assertEquals(
            "Sala Verde",
            service.calculateClassroom(
                    LocalDate.of(2023, 6, 30),
                    2026,
                    "school",
                    ClassroomShift.MANANA,
                    "Sala Verde"
            )
    );

    assertEquals(
            "Sala Azul",
            service.calculateClassroom(
                    LocalDate.of(2023, 6, 30),
                    2026,
                    "school",
                    ClassroomShift.MANANA,
                    "Sala Azul"
            )
    );

    assertThrows(
            ResponseStatusException.class,
            () -> service.calculateClassroom(
                    LocalDate.of(2023, 6, 30),
                    2026,
                    "school",
                    ClassroomShift.MANANA,
                    "Sala Roja"
            )
    );
  }

  @Test
  void julyBirthDateUsesPreviousAgeAtCutoff() {

    var repo = mock(ClassroomConfigRepository.class);

    when(repo.findByTenantId("school")).thenReturn(
            List.of(room("1", "Sala de 2", 2))
    );

    var service = createService(repo);

    assertEquals(
            2,
            service.eligibleClassrooms(
                    LocalDate.of(2023, 7, 1),
                    2026,
                    "school",
                    ClassroomShift.MANANA
            ).ageAtCutoff()
    );
  }
}
