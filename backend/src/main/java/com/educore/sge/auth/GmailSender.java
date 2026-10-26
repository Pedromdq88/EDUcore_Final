package com.educore.sge.auth;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/** Envía desde UNA cuenta institucional Gmail autorizada mediante OAuth2, no desde cada usuario. */
@Service
public class GmailSender {
    @Value("${educore.gmail.client-id:}") private String clientId;
    @Value("${educore.gmail.client-secret:}") private String clientSecret;
    @Value("${educore.gmail.refresh-token:}") private String refreshToken;
    @Value("${educore.gmail.sender:}") private String sender;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public GmailSender(JsonMapper json) { this.json = json; }

    public void send(String recipient, String subject, String body) {
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank() || sender.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gmail API no configurada");
        }
        if (!recipient.matches("^[^\\s@\\r\\n]+@[^\\s@\\r\\n]+\\.[^\\s@\\r\\n]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo inválido");
        }
        try {
            String form = "client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret)
                + "&refresh_token=" + enc(refreshToken) + "&grant_type=refresh_token";
            var tokenRequest = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            var tokenResponse = http.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() != 200) throw new IllegalStateException("OAuth2 devolvió " + tokenResponse.statusCode());
            String accessToken = json.readTree(tokenResponse.body()).path("access_token").asText();
            if (accessToken.isBlank()) throw new IllegalStateException("Sin access_token");

            String encodedSubject = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8)) + "?=";
            String encodedBody = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(body.getBytes(StandardCharsets.UTF_8));
            String mime = "From: " + sender + "\r\nTo: " + recipient + "\r\nSubject: " + encodedSubject
                + "\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8"
                + "\r\nContent-Transfer-Encoding: base64\r\n\r\n" + encodedBody;
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(mime.getBytes(StandardCharsets.UTF_8));
            String payload = json.writeValueAsString(Map.of("raw", raw));
            var request = HttpRequest.newBuilder(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Gmail devolvió " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Envío interrumpido");
        } catch (Exception e) {
            // No registrar el cuerpo de la respuesta ni los tokens OAuth (contienen secretos).
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo enviar el correo con Gmail", e);
        }
    }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
