package com.educore.sge.shared.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Validación en la API: nunca confiar solamente en formularios Angular. */
public final class RecordValidation {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private RecordValidation() {}

    public static String requiredEmail(String value, String field) {
        String email = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || email.length() > 150 || !EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ingresá un correo válido y obligatorio para " + field + ".");
        }
        return email;
    }

    public static String optionalEmail(String value, String field) {
        return value == null || value.isBlank() ? "" : requiredEmail(value, field);
    }

    /** Solo compara DNI numéricos reales; no confunde 'en trámite' o 'sin DNI'. */
    public static String comparableDni(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (!trimmed.matches("[0-9.\\- ]+")) return "";
        String digits = trimmed.replaceAll("\\D", "");
        return digits.length() >= 6 && digits.length() <= 12 ? digits : "";
    }

    /** Solo números, sin +, guiones ni espacios; admite vacío cuando el campo es opcional. */
    public static String optionalPhone(String value, String field) {
        String phone = value == null ? "" : value.trim();
        if (!phone.isEmpty() && !phone.matches("[0-9]{6,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El teléfono de " + field + " debe tener entre 6 y 20 números, sin espacios ni símbolos.");
        }
        return phone;
    }

    public static String requiredDni(String value, String field) {
        String dni = value == null ? "" : value.trim();
        if (!dni.matches("[0-9]{6,12}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El DNI de " + field + " debe contener entre 6 y 12 números, sin puntos ni letras.");
        }
        return dni;
    }

    public static String requiredText(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " es obligatorio (máximo " + maxLength + " caracteres).");
        }
        return normalized;
    }
}
