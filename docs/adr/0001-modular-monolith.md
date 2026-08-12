# ADR-0001 — Monolito modular, arquitectura hexagonal y DDD táctico

**Estado:** Aprobado  
**Fecha:** 2026-06-28  
**Decisión:** construir EduCore SGE como monolito modular con arquitectura hexagonal y DDD táctico.

---

## Contexto

EduCore SGE es un SaaS de gestión escolar con múltiples bounded contexts y un horizonte de evolución de largo plazo. El producto necesita límites de dominio claros, transacciones simples y bajo costo operativo durante sus primeras etapas.

## Decisión

- El backend será un único despliegue organizado como monolito modular.
- Los módulos se alinearán con los bounded contexts definidos en el Product Blueprint.
- Cada módulo separará `domain`, `application`, `infrastructure` y `web`.
- Dominio y aplicación no dependerán de Spring, JPA, HTTP, AWS, Redis, RabbitMQ ni proveedores externos.
- Los módulos se comunicarán mediante casos de uso públicos, eventos o facades explícitas.
- No se accederá desde un módulo a entidades JPA de otro módulo.
- Los application services delimitarán las transacciones.
- Se aplicará DDD táctico mediante aggregates, value objects, domain services y domain events donde aporten invariantes reales.

## Alternativas descartadas

### Monolito por capas tradicional

Se descarta como arquitectura oficial porque favorece controladores con lógica, servicios anémicos, acoplamiento a persistencia y límites de dominio débiles.

### Microservicios desde el inicio

Se descartan por su complejidad operativa, consistencia distribuida, mayor costo de CI/CD y menor velocidad para descubrir el dominio.

## Consecuencias

- El despliegue y las transacciones se mantienen simples.
- Los boundaries deben verificarse mediante estructura, revisiones y tests de arquitectura.
- Una separación futura de módulos requerirá evidencia operativa y un ADR específico.
- No se introducen microservicios sin una nueva decisión arquitectónica aprobada.
