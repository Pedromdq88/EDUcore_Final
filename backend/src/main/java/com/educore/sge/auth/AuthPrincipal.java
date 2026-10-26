package com.educore.sge.auth;

public record AuthPrincipal(String id, String tenantId, String email, String role, String personType, String personId) {}
