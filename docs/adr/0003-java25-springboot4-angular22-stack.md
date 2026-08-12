# ADR-0003 — Stack Java/Spring/Angular/PostgreSQL y política de fallback

**Estado:** Aprobado  
**Fecha:** 2026-06-28  
**Decisión:** usar stack objetivo moderno 2026 con fallback explícito y documentado antes del setup.

---

## Fallback activo

**Registrado:** 2026-06-29  
**Versión elegida para el bootstrap:** Java 21 LTS con Spring Boot 4.0.7.

### Causa

El ambiente de desarrollo y build disponible para iniciar el proyecto cuenta con Eclipse Temurin Java 21 LTS y no dispone de Java 25. Mantener Java 25 impediría ejecutar localmente la compilación y la validación obligatoria del backend.

### Impacto

- El bytecode y la configuración de Maven usarán Java 21.
- No se utilizarán APIs ni language features posteriores a Java 21.
- La arquitectura, Spring Boot 4.x, PostgreSQL 18, Angular 22, multi-tenancy y RLS no cambian.
- El backend deberá permanecer compatible con una actualización incremental a Java 25.

### Reevaluación

Revisar la disponibilidad de Java 25 antes de cerrar Fase 2 y, como fecha límite, el 2026-09-30. Si el ambiente y las dependencias están validados, crear un ticket independiente para actualizar compilación, CI y documentación a Java 25.

---

## Contexto

EduCore SGE se inicia como SaaS multi-tenant con horizonte de evolución de diez años. Conviene arrancar con versiones modernas y soportadas, pero sin bloquear la implementación si una pieza del ecosistema presenta incompatibilidades reales al momento de crear el proyecto.

---

## Decisión

El stack objetivo es:

- Java 25 LTS.
- Spring Boot 4.x estable.
- Angular 22.x.
- PostgreSQL 18.x.
- Redis 7.x/8.x estable administrado.
- RabbitMQ 4.x o versión compatible con Amazon MQ.

El fallback solo puede aplicarse antes del setup inicial y debe quedar reflejado en este ADR, `pom.xml`, `package.json`, Docker, CI y README.

---

## Tabla de fallback obligatorio

| Componente | Versión objetivo | Versión mínima aceptable | Condición de revisión obligatoria |
|---|---:|---:|---|
| Java | 25 LTS | 21 LTS | Usar Java 21 solo si imagen base, CI, hosting, agente de build, librería crítica o plugin Maven no soporta Java 25 de forma estable al iniciar. Registrar causa, impacto y fecha de reevaluación. |
| Spring Boot | 4.x estable | 3.5.x estable | Usar 3.5.x solo si Spring Boot 4.x, Jakarta EE 11, Spring Framework 7, springdoc, Flyway, Hibernate, Testcontainers, observabilidad o seguridad presentan incompatibilidad bloqueante. Incluir plan de migración a 4.x. |
| Angular | 22.x | 21.x LTS | Usar Angular 21 solo si Angular 22, Angular Material/CDK, NgRx Signal Store, tooling de testing o build pipeline tienen breaking changes no resueltos. Indicar fecha de reevaluación. |
| PostgreSQL | 18.x | 17.x | Usar PostgreSQL 17 solo si AWS RDS/Aurora, imagen Docker corporativa, extensión requerida o política cloud todavía no soporta PostgreSQL 18 en la región/ambiente objetivo. Mantener DDL compatible con 17 y 18. |
| Redis | 7.x/8.x estable administrado | 7.x | Elegir 7.x si el proveedor administrado no ofrece 8.x estable o si las librerías cliente no están validadas. |
| RabbitMQ | 4.x estable o Amazon MQ compatible | 3.13.x | Usar 3.13.x si Amazon MQ o docker-compose local aún no ofrecen 4.x estable compatible. |

---

## Consecuencias

- Codex no puede elegir versiones por conveniencia local.
- No se permite mezclar documentación target con implementación fallback.
- La arquitectura no cambia por usar fallback: modular monolith, hexagonal architecture, shared schema + `tenant_id`, RLS, REST y Angular standalone siguen siendo obligatorios.
- El fallback debe revisarse antes de cerrar Fase 2.

---

## Revisión obligatoria

Antes de terminar Fase 2, el equipo debe revisar si el fallback sigue siendo necesario. Si no lo es, se debe crear ticket de upgrade incremental.
