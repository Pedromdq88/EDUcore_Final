package com.educore.sge.auth.mail;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailProofServiceTest {
    private final GmailApiSender gmail = mock(GmailApiSender.class);
    private final EmailProofService proof = new EmailProofService(gmail, new BCryptPasswordEncoder(4));

    @Test void correoQueNoCambiaNoNecesitaNuevoCodigo() {
        assertDoesNotThrow(() -> proof.requireProofForChange("school","director@example.com",
                "contacto@escuela.edu.ar", "CONTACTO@ESCUELA.EDU.AR"));
    }

    @Test void correoNuevoRequiereConfirmarCodigoRecibido() {
        when(gmail.isReady()).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> proof.requireProofForChange("school",
                "director@example.com", "anterior@escuela.edu.ar", "nuevo@escuela.edu.ar"));
        proof.request("school", "director@example.com", "nuevo@escuela.edu.ar");
        var code = ArgumentCaptor.forClass(String.class);
        verify(gmail).sendEmailVerificationCode(eq("nuevo@escuela.edu.ar"), code.capture());
        assertFalse(proof.verify("school", "director@example.com", "nuevo@escuela.edu.ar", "000000x"));
        assertTrue(proof.verify("school", "director@example.com", "nuevo@escuela.edu.ar", code.getValue()));
        assertDoesNotThrow(() -> proof.requireProofForChange("school", "director@example.com",
                "anterior@escuela.edu.ar", "nuevo@escuela.edu.ar"));
        proof.consume("school", "director@example.com", "nuevo@escuela.edu.ar");
        assertThrows(ResponseStatusException.class, () -> proof.requireProofForChange("school",
                "director@example.com", "anterior@escuela.edu.ar", "nuevo@escuela.edu.ar"));
    }
}
