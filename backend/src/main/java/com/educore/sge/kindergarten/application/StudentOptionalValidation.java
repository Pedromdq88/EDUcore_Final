package com.educore.sge.kindergarten.application;

import com.educore.sge.kindergarten.infrastructure.entity.StudentJpaEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class StudentOptionalValidation {
    private StudentOptionalValidation() {}

    public static void validate(StudentJpaEntity student) {
        if (!Boolean.TRUE.equals(student.getHasForeignDocument())) {
            student.setForeignDocumentType(null);
            student.setForeignDocumentNumber(null);
        }
        student.setLegajoNumber(optional(student.getLegajoNumber(), "Legajo", 50, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 ./#_-]+$"));
        student.setCuil(optional(student.getCuil(), "CUIL", 11, "^[0-9]{11}$"));
        student.setForeignDocumentType(optional(student.getForeignDocumentType(), "Tipo de documento extranjero", 20, "^(?=.*[\\p{L}])[\\p{L}\\p{M} .,'’/-]+$"));
        student.setForeignDocumentNumber(optional(student.getForeignDocumentNumber(), "Número de documento extranjero", 20, "^[A-Za-z0-9]+(?:[ .\\/-][A-Za-z0-9]+)*$"));
        student.setNationality(optional(student.getNationality(), "Nacionalidad", 25, "^(?=.*[\\p{L}])[\\p{L}\\p{M} .,'’/-]+$"));
        student.setBirthProvince(optional(student.getBirthProvince(), "Provincia de nacimiento", 25, "^(?=.*[\\p{L}])[\\p{L}\\p{M}0-9 .,'’-]+$"));
        student.setBirthDistrict(optional(student.getBirthDistrict(), "Distrito de nacimiento", 25, "^(?=.*[\\p{L}])[\\p{L}\\p{M}0-9 .,'’-]+$"));
        student.setBirthLocality(optional(student.getBirthLocality(), "Localidad de nacimiento", 25, "^(?=.*[\\p{L}])[\\p{L}\\p{M}0-9 .,'’-]+$"));
        student.setStreet(optional(student.getStreet(), "Calle", 25, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 .,'’º°ª()/#:&-]+$"));
        student.setStreetNumber(optional(student.getStreetNumber(), "Número de domicilio", 20, "(?iu)^ *(?:[0-9]{1,6}(?: *[a-z]| *bis)?|s\\/?n|sin n[uú]mero) *$"));
        student.setFloor(optional(student.getFloor(), "Piso", 10, "(?iu)^ *(?:-?[0-9]{1,2}(?:[º°ª]|er|do|to)?|p\\.?b\\.?|ss|ep|subsuelo|entrepiso) *$"));
        student.setTower(optional(student.getTower(), "Torre", 20, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 .,'’º°ª()/#:&-]+$"));
        student.setApartment(optional(student.getApartment(), "Departamento", 10, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 ./#_-]+$"));
        student.setBetweenStreets(optional(student.getBetweenStreets(), "Entre calles", 25, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 .,'’º°ª()/#:&-]+$"));
        student.setAddressDistrict(optional(student.getAddressDistrict(), "Distrito de domicilio", 25, "^(?=.*[\\p{L}])[\\p{L}\\p{M}0-9 .,'’-]+$"));
        student.setAddressLocality(optional(student.getAddressLocality(), "Localidad de domicilio", 25, "^(?=.*[\\p{L}])[\\p{L}\\p{M}0-9 .,'’-]+$"));
        student.setOtherAddressDetails(optional(student.getOtherAddressDetails(), "Detalles del domicilio", 25, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 .,'’º°ª()/#:&-]+$"));
        student.setStudentLandlinePhone(optional(student.getStudentLandlinePhone(), "Teléfono fijo", 20, "^[0-9]{6,20}$"));
        student.setStudentCellphone(optional(student.getStudentCellphone(), "Celular", 20, "^[0-9]{6,20}$"));
        student.setTransportationMethods(optional(student.getTransportationMethods(), "Medio de transporte", 100, "^(?=.*[\\p{L}])[\\p{L}\\p{M} ,_/-]+$"));
        student.setBloodType(optional(student.getBloodType(), "Grupo sanguíneo", 3, "^(?:A|B|AB|O)[+-]$"));
        student.setHealthInsurance(optional(student.getHealthInsurance(), "Cobertura médica", 50, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 .,'’º°ª()/#:&+%_-]+$"));
        student.setAllergies(optional(student.getAllergies(), "Alergias", 255, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 .,'’º°ª()/#:&+%_-]+$"));
        student.setBirthPlace(optional(student.getBirthPlace(), "Lugar de nacimiento", 100, "^(?=.*[\\p{L}])[\\p{L}\\p{M}0-9 .,'’-]+$"));
        student.setAddress(optional(student.getAddress(), "Domicilio", 255, "^(?=.*[\\p{L}0-9])[\\p{L}\\p{M}0-9 .,'’º°ª()/#:&-]+$"));
        student.setGender(optional(student.getGender(), "Género", 30, "^(?=.*[\\p{L}])[\\p{L}\\p{M} .,'’/-]+$"));
        optional(student.getDocumentNumber(), "DNI", 8, "[0-9]{6,8}");
        count(student.getSiblingCount(), "Cantidad de hermanos");
        count(student.getSiblingsInThisSchool(), "Hermanos en esta escuela");
        if (student.getSiblingsInThisSchool() != null && student.getSiblingsInThisSchool() >
                (student.getSiblingCount() == null ? 0 : student.getSiblingCount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los hermanos en esta escuela no pueden superar el total.");
        }
    }

    private static String optional(String value, String field, int max, String pattern) {
        if (value == null || value.isEmpty()) return value;
        if (value.length() > max)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + ": máximo " + max + " caracteres.");
        if (value.isBlank() || !value.matches(pattern))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + ": formato inválido. Corregí el dato o dejalo vacío.");
        return value.trim();
    }

    private static void count(Integer value, String field) {
        if (value != null && (value < 0 || value > 30))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + ": ingresá un valor entre 0 y 30.");
    }
}
