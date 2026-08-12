# ADR-0005 — Eventos internos, outbox y RabbitMQ

**Estado:** Aprobado  
**Fecha:** 2026-06-28  
**Decisión:** usar eventos internos dentro del monolito y transactional outbox para efectos externos durables, con RabbitMQ como broker asíncrono.

---

## Contexto

Casos de uso como email, WhatsApp, PDFs, importaciones, webhooks de pagos y notificaciones masivas no deben acoplar la transacción de dominio a proveedores externos ni perderse ante fallos parciales.

## Decisión

- Las reacciones internas usarán domain events mediante Spring Events o un publisher propio compartido.
- Todo efecto externo durable se registrará en la misma transacción de negocio mediante el patrón outbox.
- `outbox_events` incluirá `tenant_id`, `event_type`, `aggregate_type`, `aggregate_id`, `payload`, `status`, `attempts`, `available_at` y `created_at`.
- Un procesador publicará los eventos pendientes de forma asíncrona.
- RabbitMQ se usará para procesamiento asíncrono confiable e integración desacoplada.
- Los consumidores deberán tolerar reintentos y procesar mensajes de forma idempotente.
- Se observarán la edad del outbox, los reintentos y las colas atascadas.

## Alternativas descartadas

- Invocar proveedores externos dentro de la transacción de negocio.
- Publicar mensajes sin persistencia transaccional previa.
- Kafka para esta etapa del producto.
- Convertir los módulos en microservicios para obtener asincronía.

## Consecuencias

- Los efectos externos pueden reintentarse sin perder la intención del dominio.
- La entrega es al menos una vez, por lo que los consumidores requieren idempotencia.
- El outbox y RabbitMQ agregan operación y monitoreo, pero no cambian el modelo de despliegue como monolito modular.
