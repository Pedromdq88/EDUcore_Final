# ADR-0006 — Autenticación RS256, refresh token rotation y fuente de verdad de sesiones

**Estado:** Aprobado  
**Fecha:** 2026-06-28  
**Decisión:** JWT access tokens firmados con RS256; refresh tokens opacos, rotativos, hasheados; PostgreSQL como fuente de verdad; Redis como cache auxiliar.

---

## Contexto

El sistema necesita autenticación segura para usuarios de colegios, familias, alumnos y plataforma. Los refresh tokens deben permitir sesiones persistentes sin guardar tokens en localStorage/sessionStorage y sin perder capacidad de revocación, auditoría o detección de reuse.

Existía una ambigüedad: `AGENTS.md` mencionaba Redis para refresh tokens, mientras el roadmap incluía `refresh_token_sessions` en PostgreSQL. Esta ADR cierra la decisión.

---

## Decisión

### Access token

- JWT firmado con RS256.
- TTL corto: 10 a 15 minutos.
- Claims mínimos: `sub`, `tenant_id`, `tenant_slug`, `roles`, `permissions`, `session_id`, `iat`, `exp`.
- El frontend guarda el access token solo en memoria.

### Refresh token

- Token opaco, aleatorio y de alta entropía.
- Se envía en cookie `HttpOnly`, `Secure`, `SameSite=Lax` o `Strict` según portal.
- Nunca se guarda plano.
- Se guarda solo `token_hash`.
- Rota en cada uso.
- Detecta reuse.

### Fuente de verdad

PostgreSQL es la fuente de verdad para:

- sesión activa;
- expiración;
- revocación;
- reuse detection;
- auditoría;
- invalidación por logout;
- invalidación por cambio de password;
- invalidación por bloqueo de usuario;
- invalidación masiva por incidente.

Redis solo puede usarse como:

- cache `token_hash -> session_id`;
- cache de metadata no sensible;
- rate limiting;
- revocation hint para access tokens si se decide validar `session_id` en endpoints críticos.

Una entrada positiva en Redis no autoriza refresh por sí sola. La emisión de nuevos tokens siempre requiere transacción en PostgreSQL.

---

## Tabla `refresh_token_sessions`

Campos mínimos:

- `id UUID`
- `tenant_id UUID`
- `user_id UUID`
- `token_hash VARCHAR(255)`
- `family_id UUID`
- `status VARCHAR(30)`
- `expires_at TIMESTAMPTZ`
- `revoked_at TIMESTAMPTZ NULL`
- `replaced_by_session_id UUID NULL`
- `reuse_detected_at TIMESTAMPTZ NULL`
- `ip_address INET NULL`
- `user_agent TEXT NULL`
- `created_at TIMESTAMPTZ`

Constraints:

- `UNIQUE (tenant_id, id)`
- `UNIQUE (tenant_id, token_hash)`
- FK `(tenant_id, user_id)` hacia `user_accounts(tenant_id, id)`
- índice `(tenant_id, user_id, status)`
- índice `(tenant_id, family_id)`
- índice `(expires_at)` para limpieza

---

## Flujo login

1. Validar credenciales sin filtrar si el email existe.
2. Crear access token.
3. Crear refresh token opaco.
4. Hashear refresh token.
5. Insertar sesión en PostgreSQL.
6. Poblar Redis de forma best-effort si está disponible.
7. Devolver access token en body y refresh token en cookie HttpOnly.

---

## Flujo refresh

1. Leer refresh token desde cookie.
2. Hashear token recibido.
3. Buscar sesión en PostgreSQL, usando Redis solo como ayuda de lookup si existe.
4. Si no existe, está expirada o está revocada, rechazar.
5. Si está revocada y pertenece a una familia activa, marcar reuse detection y revocar toda la familia.
6. En una única transacción:
   - revocar sesión anterior;
   - crear sesión nueva;
   - crear nuevo access token;
   - crear nuevo refresh token.
7. Después del commit, invalidar Redis del token viejo y cachear token nuevo best-effort.

---

## Flujo logout

1. Revocar sesión en PostgreSQL.
2. Invalidar Redis best-effort.
3. Borrar cookie refresh en response.

---

## Fallos

- Si Redis está caído, refresh puede seguir usando PostgreSQL y debe emitir alerta operativa.
- Si PostgreSQL está caído, refresh falla cerrado.
- Si Redis contiene una sesión activa pero PostgreSQL la marca revocada, gana PostgreSQL.

---

## Consecuencias

- Mayor robustez de auditoría y revocación.
- Redis deja de ser punto único de verdad.
- El refresh endpoint requiere transacciones correctas y tests de concurrencia.
- Codex no puede implementar refresh token solo en Redis.
