package com.educore.sge.shared;

public class InstitutionContext {
    private static final ThreadLocal<String> CURRENT_INSTITUTION = new ThreadLocal<>();

    public static void setCurrentInstitution(String institutionId) {
        CURRENT_INSTITUTION.set(institutionId);
    }

    public static String getCurrentInstitution() {
        return CURRENT_INSTITUTION.get();
    }

    public static void clear() {
        CURRENT_INSTITUTION.remove();
    }
}
