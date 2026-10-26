package com.educore.sge.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT HS256. En el perfil local genera y conserva automaticamente la clave,
 * sin pedir configuraciones manuales. En produccion exige una clave externa.
 */
@Service
public class JwtTokenService {
    private static final String ISSUER = "educore-sge";
    private static final String AUDIENCE = "educore-web";
    private static final Duration LIFETIME = Duration.ofHours(12);

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenService(@Value("${educore.jwt.secret-base64:}") String secretBase64,
                           Environment environment) {
        String configured = secretBase64 == null ? "" : secretBase64.trim();
        boolean local = environment.acceptsProfiles(Profiles.of("local", "test"));
        byte[] key;

        if (configured.isBlank()) {
            if (!local) {
                throw new IllegalStateException(
                        "En produccion hay que configurar EDUCORE_JWT_SECRET_BASE64. " +
                                "En desarrollo local se genera automaticamente.");
            }
            key = loadOrCreateLocalKey();
        } else {
            try {
                key = Base64.getDecoder().decode(configured);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("La clave JWT configurada no es Base64 valido", ex);
            }
        }
        if (key.length < 32) {
            throw new IllegalStateException("La clave JWT debe tener 32 bytes como minimo");
        }
        this.algorithm = Algorithm.HMAC256(key);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).withAudience(AUDIENCE).build();
    }

    private static byte[] loadOrCreateLocalKey() {
        // Se guarda FUERA del proyecto y de Git para reutilizar los JWT tras reinicios.
        Path file = Path.of(System.getProperty("user.home"), ".educore", "jwt-signing-key.base64");
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                byte[] randomKey = new byte[32];
                new SecureRandom().nextBytes(randomKey);
                String encoded = Base64.getEncoder().encodeToString(randomKey);
                try {
                    Files.writeString(file, encoded, StandardCharsets.US_ASCII,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                } catch (FileAlreadyExistsException ignored) {
                    // Otro proceso ya genero el archivo. Leer su contenido.
                }
            }
            String saved = Files.readString(file, StandardCharsets.US_ASCII).trim();
            byte[] result = Base64.getDecoder().decode(saved);
            if (result.length < 32) {
                throw new IllegalStateException("La clave JWT local guardada es invalida: " + file);
            }
            return result;
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("No se pudo crear o leer la clave JWT local en " + file, ex);
        }
    }

    public String createAccessToken(AuthAccount account) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withSubject(account.id)
                .withClaim("tenant_id", account.tenantId)
                .withClaim("role", account.role)
                .withClaim("person_type", account.personType)
                .withClaim("person_id", account.personId)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(LIFETIME)))
                .sign(algorithm);
    }

    public DecodedJWT verifyAccessToken(String token) throws JWTVerificationException {
        if (token == null || token.length() > 4096 || token.isBlank()) {
            throw new com.auth0.jwt.exceptions.JWTDecodeException("Token JWT invalido");
        }
        DecodedJWT jwt = verifier.verify(token);
        if (jwt.getSubject() == null || jwt.getSubject().isBlank()
                || jwt.getId() == null || jwt.getId().isBlank()
                || jwt.getExpiresAt() == null || jwt.getIssuedAt() == null
                || jwt.getClaim("tenant_id").asString() == null
                || jwt.getClaim("role").asString() == null) {
            throw new com.auth0.jwt.exceptions.JWTDecodeException("JWT incompleto");
        }
        return jwt;
    }

    public Instant expiration(String token) {
        return verifyAccessToken(token).getExpiresAt().toInstant();
    }
}
