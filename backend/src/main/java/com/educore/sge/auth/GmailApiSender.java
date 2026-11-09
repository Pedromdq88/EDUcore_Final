
package com.educore.sge.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
public class GmailApiSender {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final ObjectMapper json;
    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String fromAddress;

    public GmailApiSender(
            ObjectMapper json,
            @Value("${educore.gmail.enabled:false}") boolean enabled,
            @Value("${educore.gmail.client-id:}") String clientId,
            @Value("${educore.gmail.client-secret:}") String clientSecret,
            @Value("${educore.gmail.refresh-token:}") String refreshToken,
            @Value("${educore.gmail.from-address:}") String fromAddress
    ) {
        this.json = json;
        this.enabled = enabled;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.fromAddress = fromAddress;
    }

    public boolean isReady() {
        return enabled
                && !clientId.isBlank()
                && !clientSecret.isBlank()
                && !refreshToken.isBlank()
                && fromAddress.matches(
                "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"
        );
    }

    public void sendResetCode(String recipient, String code) {

        if (!isReady()) {
            throw new IllegalStateException(
                    "Gmail no está configurado en el servidor"
            );
        }

        if (recipient == null ||
                !recipient.matches("^[^\\r\\n@]+@[^\\r\\n@]+$")) {
            throw new IllegalArgumentException(
                    "Destinatario inválido"
            );
        }

        try {
            // 1. Obtener Access Token mediante OAuth2
            String accessToken = fetchAccessToken();

            // 2. Construir el mensaje
            String body =
                    "Recibimos una solicitud para recuperar " +
                            "tu contraseña de EDUcore.\r\n\r\n" +
                            "Tu código es: " + code + "\r\n\r\n" +
                            "Vence en 10 minutos. " +
                            "Solo podés utilizarlo una vez.\r\n" +
                            "Si no solicitaste este cambio, " +
                            "ignorá este mensaje.\r\n";

            String subject = "=?UTF-8?B?" +
                    Base64.getEncoder().encodeToString(
                            "EDUcore - Recuperación de contraseña"
                                    .getBytes(StandardCharsets.UTF_8)
                    ) + "?=";

            // 3. Construir correo MIME
            String mime =
                    "From: " + fromAddress + "\r\n" +
                            "To: " + recipient + "\r\n" +
                            "Subject: " + subject + "\r\n" +
                            "MIME-Version: 1.0\r\n" +
                            "Content-Type: text/plain; charset=UTF-8\r\n" +
                            "Content-Transfer-Encoding: base64\r\n\r\n" +
                            Base64.getMimeEncoder(
                                    76,
                                    "\r\n".getBytes(StandardCharsets.US_ASCII)
                            ).encodeToString(
                                    body.getBytes(StandardCharsets.UTF_8)
                            );

            // 4. Codificar mensaje para Gmail API
            String raw = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            mime.getBytes(StandardCharsets.UTF_8)
                    );

            String payload = json.createObjectNode()
                    .put("raw", raw)
                    .toString();

            // 5. Enviar correo mediante Gmail API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
                    ))
                    .timeout(Duration.ofSeconds(15))
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
                    .header(
                            "Content-Type",
                            "application/json; charset=utf-8"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(payload)
                    )
                    .build();

            HttpResponse<String> sent = http.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (sent.statusCode() / 100 != 2) {
                throw new IOException(
                        "Gmail API respondió HTTP " +
                                sent.statusCode()
                );
            }

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No se pudo entregar el correo de recuperación",
                    ex
            );

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Envío interrumpido",
                    ex
            );
        }
    }

    private String fetchAccessToken()
            throws IOException, InterruptedException {

        // 1. Preparar credenciales OAuth2
        String params =
                "client_id=" + encode(clientId) +
                        "&client_secret=" + encode(clientSecret) +
                        "&refresh_token=" + encode(refreshToken) +
                        "&grant_type=refresh_token";

        // 2. Solicitar Access Token a Google
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://oauth2.googleapis.com/token"
                ))
                .timeout(Duration.ofSeconds(15))
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(params)
                )
                .build();

        HttpResponse<String> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // 3. Diagnosticar errores OAuth2 sin exponer tokens
        if (response.statusCode() / 100 != 2) {

            String errorCode = "desconocido";

            try {
                JsonNode errorResponse =
                        json.readTree(response.body());

                String candidate = errorResponse
                        .path("error")
                        .asText("");

                if (candidate.matches("[a-z_]+")) {
                    errorCode = candidate;
                }

            } catch (Exception ignored) {
                // No imprimir datos sensibles de Google
            }

            throw new IOException(
                    "OAuth2 respondió HTTP " +
                            response.statusCode() +
                            " - Error: " + errorCode
            );
        }

        // 4. Extraer Access Token
        JsonNode access = json.readTree(response.body())
                .get("access_token");

        if (access == null || access.asText().isBlank()) {
            throw new IOException(
                    "OAuth2 no devolvió un access_token"
            );
        }

        return access.asText();
    }

    private static String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
