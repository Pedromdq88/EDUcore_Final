package com.educore.sge.auth.mail;

public record EmailDispatchRequested(String recipient, String subject, String body) {}
