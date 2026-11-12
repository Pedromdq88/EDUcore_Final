package com.educore.sge.auth;

import com.educore.sge.kindergarten.infrastructure.entity.RelationshipType;
import com.educore.sge.kindergarten.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.shared.Rol;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTutorTest {
    @Test
    void createsLinkedTutorWithEncodedDniAndMandatoryChange() {
        var accounts = mock(AuthUserRepository.class);
        var sessions = mock(AuthSessionRepository.class);
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
        var service = new AuthService(accounts, sessions, encoder);
        var tutor = new TutorJpaEntity();
        tutor.setId("tutor-01");
        tutor.setTenantId("school-01");
        tutor.setFirstName("Nombre");
        tutor.setLastName("Apellido");
        tutor.setEmail(" Tutor@Escuela.Com ");
        tutor.setDocumentNumber("12.345.678");
        tutor.setRelationship(RelationshipType.MADRE);
        when(accounts.findByTenantIdAndTutorId("school-01", "tutor-01")).thenReturn(Optional.empty());
        when(accounts.findByTenantIdAndEmailIgnoreCase("school-01", "tutor@escuela.com")).thenReturn(Optional.empty());
        when(accounts.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(service.provisionTutorAccount(tutor, "school-01"));
        var captor = org.mockito.ArgumentCaptor.forClass(AuthUser.class);
        verify(accounts).save(captor.capture());
        var created = captor.getValue();
        assertEquals("tutor-01", created.tutorId);
        assertEquals("school-01", created.tenantId);
        assertEquals("tutor@escuela.com", created.email);
        assertEquals(Rol.TUTOR, created.role);
        assertTrue(created.enabled);
        assertTrue(created.mustChangePassword);
        assertNotEquals("12345678", created.passwordHash);
        assertTrue(encoder.matches("12345678", created.passwordHash));
    }
}
