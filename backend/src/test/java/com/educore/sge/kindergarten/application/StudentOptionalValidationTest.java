package com.educore.sge.kindergarten.application;

import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class StudentOptionalValidationTest {
    @Test
    void emptyOptionalFieldsAreValid() {
        assertDoesNotThrow(() -> StudentOptionalValidation.validate(new StudentJpaEntity()));
    }

    @Test
    void rejectsDepartmentBeforeDatabaseTruncation() {
        var student = new StudentJpaEntity();
        student.setApartment("12345678901");
        var error = assertThrows(ResponseStatusException.class, () -> StudentOptionalValidation.validate(student));
        assertEquals(400, error.getStatusCode().value());
        assertEquals("Departamento: máximo 10 caracteres.", error.getReason());
        student.setApartment("2A");
        assertDoesNotThrow(() -> StudentOptionalValidation.validate(student));
    }

    @Test
    void validatesBirthplaceAndAddressFormats() {
        var student = new StudentJpaEntity();
        student.setBirthDistrict("General Pueyrredón");
        student.setBirthLocality("9 de Julio");
        student.setStreet("Av. Colón");
        student.setStreetNumber("123 bis");
        student.setFloor("PB");
        student.setBloodType("AB-");
        assertDoesNotThrow(() -> StudentOptionalValidation.validate(student));
        student.setBirthDistrict("123456");
        assertThrows(ResponseStatusException.class, () -> StudentOptionalValidation.validate(student));
        student.setBirthDistrict("a".repeat(26));
        assertThrows(ResponseStatusException.class, () -> StudentOptionalValidation.validate(student));
    }

    @Test
    void doesNotValidateAnInactiveForeignDocument() {
        var student = new StudentJpaEntity();
        student.setHasForeignDocument(false);
        student.setForeignDocumentNumber("###");
        StudentOptionalValidation.validate(student);
        assertNull(student.getForeignDocumentNumber());
        student.setHasForeignDocument(true);
        student.setForeignDocumentNumber("###");
        assertThrows(ResponseStatusException.class, () -> StudentOptionalValidation.validate(student));
    }
}
