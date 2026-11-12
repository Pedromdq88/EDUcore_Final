package com.educore.sge.auth.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);
    private final GmailApiSender gmail;

    public EmailNotificationListener(GmailApiSender gmail) { this.gmail = gmail; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(EmailDispatchRequested event) {
        try {
            gmail.sendNotification(event.recipient(), event.subject(), event.body());
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar una notificación de EDUcore al correo solicitado: {}", e.getClass().getSimpleName());
        }
    }
}
