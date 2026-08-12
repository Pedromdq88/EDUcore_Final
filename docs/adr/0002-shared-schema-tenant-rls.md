# ADR-0002 — Multi-tenancy con shared schema, `tenant_id` y RLS

**Estado:** Aprobado  
**Fecha:** 2026-06-28  
**Decisión:** usar PostgreSQL con base y esquema compartidos, `tenant_id` obligatorio y Row Level Security como defensa en profundidad.

---

## Contexto

EduCore SGE debe aislar datos de instituciones diferentes sin multiplicar migraciones, conexiones, backups y tareas operativas. Los datos escolares y de menores requieren controles de aislamiento en aplicación y base de datos.

## Decisión

- Un tenant representa una institución educativa contractual.
- Toda tabla de negocio tenant-scoped tendrá `tenant_id UUID NOT NULL`.
- Las claves únicas de negocio incluirán `tenant_id`.
- Los índices tenant-scoped comenzarán con `tenant_id`.
- Las relaciones críticas usarán foreign keys compuestas para impedir referencias cross-tenant.
- Los repositorios y casos de uso recibirán el tenant desde `TenantContext`, nunca desde body, query param o path.
- Las tablas críticas habilitarán y forzarán PostgreSQL RLS.
- La conexión activa establecerá `app.current_tenant` mediante `set_config` y lo reseteará al devolverse al pool.
- La aplicación usará un rol no owner; el owner quedará reservado para migraciones.
- Tests con PostgreSQL real verificarán aislamiento y ausencia de leakage entre conexiones reutilizadas.

RLS es una segunda línea de defensa. No reemplaza autorización, filtros tenant-scoped, constraints ni auditoría.

## Alternativas descartadas

- `schema-per-tenant` como estrategia predeterminada.
- `database-per-tenant` como estrategia predeterminada.
- `SET search_path` como mecanismo de multi-tenancy.
- Confiar únicamente en filtros de aplicación o Hibernate.

## Consecuencias

- Las migraciones son únicas para todos los tenants.
- Toda persistencia tenant-scoped debe mantener disciplina explícita de aislamiento.
- La ruta Enterprise puede usar infraestructura dedicada en el futuro sin eliminar `tenant_id` del dominio ni de las tablas.
