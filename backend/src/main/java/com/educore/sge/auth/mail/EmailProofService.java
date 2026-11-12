package com.educore.sge.auth.mail;

import com.educore.sge.auth.AuthService;
import com.educore.sge.shared.validation.RecordValidation;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Instant;
import java.time.Duration;

/** Valida acceso a un buzón antes de cambiar correos institucionales. No requiere DDL. */
@Service
public class EmailProofService {
    private static final Duration TTL=Duration.ofMinutes(10);
    private static final Duration VERIFIED_TTL=Duration.ofMinutes(15);
    private static final Duration COOLDOWN=Duration.ofSeconds(90);
    private final GmailApiSender gmail;
    private final PasswordEncoder passwords;
    private final SecureRandom random=new SecureRandom();
    private final Map<String,Challenge> pending=new ConcurrentHashMap<>();
    private final Map<String,Instant> proof=new ConcurrentHashMap<>();
    private final Map<String,Instant> lastSend=new ConcurrentHashMap<>();
    private final Map<String,HourWindow> hourly=new ConcurrentHashMap<>();

    private static final class Challenge {
        final String hashedCode;
        final Instant expires;
        final AtomicInteger attempts=new AtomicInteger();
        Challenge(String hashedCode,Instant expires){this.hashedCode=hashedCode;this.expires=expires;}
    }
    private static final class HourWindow {
        final Instant start;
        final AtomicInteger count=new AtomicInteger();
        HourWindow(Instant start){this.start=start;}
    }
    public EmailProofService(GmailApiSender gmail, PasswordEncoder passwords) {
        this.gmail=gmail;this.passwords=passwords;
    }
    private String key(String institution,String actor,String address) {
        return AuthService.hashToken(institution+"|"+actor+"|"+address);
    }
    public String normalized(String address) {
        return RecordValidation.requiredEmail(address,"verificación");
    }

    public synchronized void request(String institution,String actor,String address) {
        String email=normalized(address);
        if(!gmail.isReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de verificación por Gmail no está disponible.");
        String k=key(institution,actor,email);
        Instant now=Instant.now();
        if(pending.size()>10000) {pending.clear();proof.clear();lastSend.clear();hourly.clear();}
        if(lastSend.containsKey(k) && lastSend.get(k).plus(COOLDOWN).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Esperá 90 segundos antes de solicitar otro código para este correo.");
        }
        HourWindow window=hourly.compute(k,(id,prev)->prev==null || prev.start.plus(Duration.ofHours(1)).isBefore(now)
                ? new HourWindow(now):prev);
        if(window.count.incrementAndGet()>5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Se alcanzó el límite de verificaciones de este correo. Probá más tarde.");
        String code=String.format(java.util.Locale.ROOT,"%06d",random.nextInt(1_000_000));
        try {gmail.sendEmailVerificationCode(email,code);}
        catch(RuntimeException e) {throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo enviar el correo de verificación.");}
        pending.put(k,new Challenge(passwords.encode(code),now.plus(TTL)));
        proof.remove(k);
        lastSend.put(k,now);
    }

    public boolean verify(String institution,String actor,String address,String code) {
        String k=key(institution,actor,normalized(address));
        if(code==null || !code.matches("[0-9]{6}"))return false;
        Challenge challenge=pending.get(k);
        if(challenge==null)return false;
        if(challenge.expires.isBefore(Instant.now()) || challenge.attempts.incrementAndGet()>5) {
            pending.remove(k);return false;
        }
        if(!passwords.matches(code,challenge.hashedCode))return false;
        pending.remove(k);
        proof.put(k,Instant.now().plus(VERIFIED_TTL));
        return true;
    }

    public void requireProofForChange(String institution,String actor,String previous,String requested) {
        String email=normalized(requested);
        if(previous != null && email.equalsIgnoreCase(previous.trim()))return;
        Instant validUntil=proof.get(key(institution,actor,email));
        if(validUntil==null || !validUntil.isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Antes de guardar el correo "+email+", verificá el código enviado a esa dirección.");
    }
    public void consume(String institution,String actor,String address) {
        proof.remove(key(institution,actor,normalized(address)));
    }
}
