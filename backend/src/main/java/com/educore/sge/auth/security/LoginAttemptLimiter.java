package com.educore.sge.auth.security;

import com.educore.sge.auth.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory throttle. Use a shared datastore if the API scales to multiple replicas. */
@Component
public final class LoginAttemptLimiter {
    private static final long WINDOW_MS = Duration.ofMinutes(15).toMillis();
    private static final int MAX_ACCOUNT_FAILURES = 12;
    private static final int MAX_IP_FAILURES = 60;
    private final ConcurrentHashMap<String, Window> failures = new ConcurrentHashMap<>();
    private static final class Window {
        private long startedAt;
        private int failures;
        Window(long now) { startedAt = now; }
    }
    public void check(String tenant, String email, String remoteAddress) {
        long now = System.currentTimeMillis();
        if (overLimit(accountKey(tenant, email, remoteAddress), MAX_ACCOUNT_FAILURES, now)
                || overLimit(ipKey(remoteAddress), MAX_IP_FAILURES, now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos de acceso. Intentá más tarde.");
        }
    }
    public void failed(String tenant, String email, String remoteAddress) {
        long now = System.currentTimeMillis();
        increment(accountKey(tenant, email, remoteAddress), now);
        increment(ipKey(remoteAddress), now);
        if (failures.size() > 20000) failures.entrySet().removeIf(e -> now - e.getValue().startedAt > WINDOW_MS);
    }
    public void succeeded(String tenant, String email, String remoteAddress) {
        failures.remove(accountKey(tenant, email, remoteAddress));
    }
    private static String accountKey(String tenant, String email, String ip) {
        String value = (tenant == null ? "" : tenant.trim()) + "|" +
                (email == null ? "" : email.trim().toLowerCase(Locale.ROOT)) + "|" +
                (ip == null ? "" : ip);
        return "account:" + AuthService.hashToken(value);
    }
    private static String ipKey(String ip) {
        return "ip:" + AuthService.hashToken(ip == null ? "unknown" : ip);
    }
    private boolean overLimit(String key, int max, long now) {
        Window w = failures.get(key);
        if (w == null) return false;
        synchronized (w) {
            if (now - w.startedAt >= WINDOW_MS) {w.startedAt=now;w.failures=0;return false;}
            return w.failures >= max;
        }
    }
    private void increment(String key, long now) {
        Window w = failures.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            if (now - w.startedAt >= WINDOW_MS) {w.startedAt=now;w.failures=0;}
            w.failures++;
        }
    }
}
