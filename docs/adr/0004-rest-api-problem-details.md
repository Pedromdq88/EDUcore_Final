# ADR-0004 — API REST versionada y errores Problem Details

**Estado:** Aprobado  
**Fecha:** 2026-06-28  
**Decisión:** exponer una API REST versionada con DTOs explícitos y errores basados en `ProblemDetail`.

---

## Contexto

El backend necesita un contrato estable y predecible para el frontend y futuras integraciones, con validación, paginación y errores consistentes y trazables.

## Decisión

- La API usará REST con base path `/api/v1` y versionado por URL.
- Cada endpoint tendrá DTOs explícitos de request y response.
- Las entidades JPA no se expondrán por HTTP.
- Los requests se validarán con Bean Validation.
- La paginación usará `page`, `size` y `sort`.
- Los listados paginados responderán con `PageResponse<T>`.
- Los errores usarán `ProblemDetail` e incluirán `type`, `title`, `status`, `detail`, `code`, `correlationId` y `fieldErrors` cuando corresponda.
- El contrato OpenAPI será generado y revisado.
- Los controllers traducirán HTTP a commands/queries y no contendrán lógica de negocio.

## Alternativas descartadas

- GraphQL para el MVP.
- Exponer directamente entidades de persistencia.
- Formatos de error particulares por endpoint.
- Una API sin versionado explícito.

## Consecuencias

- El frontend puede manejar errores y paginación de forma uniforme.
- Los cambios incompatibles requieren una estrategia explícita de versión.
- Los errores operativos pueden correlacionarse sin exponer detalles internos sensibles.
