package com.educore.sge.kindergarten.application;

import com.educore.sge.institution.infrastructure.domain.ClassroomShift;
import com.educore.sge.institution.infrastructure.entity.ClassroomConfigJpaEntity;
import com.educore.sge.institution.infrastructure.repository.ClassroomConfigRepository;
import com.educore.sge.institution.infrastructure.repository.SchoolProfileJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassroomAssignmentEditTest {
    private final ClassroomConfigRepository rooms = mock(ClassroomConfigRepository.class);
    private final ClassroomAssignmentService service = new ClassroomAssignmentService(rooms, mock(SchoolProfileJpaRepository.class));

    @Test void enEdicionPermiteUnAulaDeOtraEdadSiExisteEnElTurno() {
        var room = new ClassroomConfigJpaEntity();
        room.setName("Sala de 5 - Mañana");
        room.setMinimumAge(5);
        room.setShift(ClassroomShift.MANANA);
        when(rooms.findByTenantId("school")).thenReturn(List.of(room));
        assertEquals("Sala de 5 - Mañana", service.selectExistingClassroom("school", ClassroomShift.MANANA, room.getName()));
    }

    @Test void rechazaAulaDeOtroTurno() {
        var room = new ClassroomConfigJpaEntity();
        room.setName("Sala de 5 - Tarde");
        room.setShift(ClassroomShift.TARDE);
        when(rooms.findByTenantId("school")).thenReturn(List.of(room));
        assertThrows(ResponseStatusException.class, () -> service.selectExistingClassroom("school", ClassroomShift.MANANA, room.getName()));
    }
}
