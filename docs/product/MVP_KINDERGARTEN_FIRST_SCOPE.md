# MVP Kindergarten-First Scope — EduCore SGE

## Estado

Propuesto para MVP inicial.

## Tipo de documento

Documento de alcance de producto.

No es un ADR porque no cambia la arquitectura, el stack, el modelo multi-tenant ni la estrategia técnica base del proyecto.

## Decisión

EduCore SGE se implementará como un Sistema de Gestión Escolar SaaS Multi-Tenant preparado para múltiples niveles educativos, pero el primer MVP funcional será validado sobre el caso de uso de **Nivel Inicial / Jardín de Infantes**.

Esto significa:

- El modelo de datos seguirá siendo genérico y multi-nivel.
- El backend no debe hardcodear reglas exclusivas de jardín.
- La base de datos no debe crear tablas específicas para jardín.
- La UI inicial podrá priorizar lenguaje y flujos de jardín.
- Primaria y secundaria quedan fuera del alcance funcional inicial, pero no fuera del diseño.

## Objetivo

Reducir el alcance inicial del MVP para hacerlo implementable, testeable y presentable por un equipo pequeño, sin comprometer la evolución futura hacia primaria y secundaria.

El objetivo no es construir un producto separado para jardín, sino validar EduCore SGE con un primer vertical educativo más simple.

## Alcance funcional del MVP Kindergarten-First

El MVP inicial se enfocará en:

1. Gestión de tenants e institución.
2. Configuración institucional básica.
3. Nivel educativo inicial.
4. Salas y secciones.
5. Alumnos.
6. Familias.
7. Tutores y responsables.
8. Autorizados a retirar.
9. Contactos de emergencia.
10. Matrícula básica.
11. Asistencia diaria.
12. Comunicados básicos.
13. Documentación del alumno.
14. Cuotas, conceptos y pagos manuales.
15. Estado de cuenta familiar.
16. Dashboard administrativo inicial.
17. Auditoría base.
18. Importación inicial de alumnos/familias desde Excel si el tiempo lo permite.

## Fuera del alcance funcional inicial

Quedan fuera del MVP kindergarten-first:

1. Calificaciones complejas.
2. Boletines académicos formales.
3. Escalas de notas.
4. Materias por docente como flujo principal.
5. Course offerings avanzados.
6. Promoción académica anual.
7. Report cards configurables.
8. Portal alumno avanzado.
9. Integraciones de pago automáticas.
10. WhatsApp Business.
11. Facturación electrónica.
12. Biblioteca.
13. IA.
14. BI avanzado.
15. Multi-sede avanzado.

Estos puntos podrán implementarse en fases posteriores, especialmente al habilitar primaria y secundaria.

## Reglas de diseño

### 1. No hardcodear jardín en backend

No crear clases, paquetes, endpoints ni entidades acopladas exclusivamente a jardín.

Evitar nombres como:

- `KindergartenStudent`
- `KindergartenSection`
- `JardinAlumnoService`
- `SalaJardinController`

Preferir nombres genéricos:

- `Student`
- `EducationLevel`
- `GradeLevel`
- `Section`
- `Enrollment`
- `AttendanceSession`

### 2. Representar jardín como configuración

Nivel Inicial debe representarse mediante datos configurables:

```yaml
education_level:
  code: INITIAL
  name: Nivel Inicial
```

Ejemplos de grade levels:

- Sala de 2.
- Sala de 3.
- Sala de 4.
- Sala de 5.

Ejemplos de sections:

- Sala de 3 - Turno Mañana.
- Sala de 4 - Turno Tarde.
- Sala de 5 - Turno Mañana.

### 3. Mantener lenguaje interno genérico

En código y base de datos se debe usar lenguaje genérico:

- `education_level`
- `grade_level`
- `section`
- `student`
- `family`
- `guardian`
- `enrollment`
- `attendance_session`

La UI puede adaptar etiquetas según el nivel:

- `INITIAL`: Sala.
- `PRIMARY`: Grado.
- `SECONDARY`: Año.

### 4. No crear tablas específicas por nivel

No crear tablas como:

- `kindergarten_students`
- `jardin_salas`
- `primary_students`
- `secondary_students`

Usar tablas generales:

- `students`
- `education_levels`
- `grade_levels`
- `sections`
- `student_enrollments`
- `families`
- `guardian_relationships`
- `attendance_sessions`

### 5. No bloquear primaria ni secundaria

Aunque el MVP valide jardín, el modelo debe permitir después:

- `PRIMARY`
- `SECONDARY`

sin rediseñar base de datos ni reescribir módulos principales.

## Modelo conceptual esperado

```text
EducationLevel
└── GradeLevel
    └── Section
        └── StudentEnrollment
            └── Student
```

Ejemplo:

```text
Nivel Inicial
└── Sala de 4
    └── Sala de 4 - Turno Mañana
        ├── Alumno A
        ├── Alumno B
        └── Alumno C
```

## Módulos prioritarios para el MVP

### Platform

Necesario para crear y administrar tenants: tenant, plan, estado, activación y bootstrap inicial.

### Identity

Necesario para usuarios, roles, permisos y sesiones: login, refresh token, roles base, permisos y usuarios administrativos.

### Institution

Necesario para configurar perfil institucional, niveles educativos, ciclos lectivos, términos o períodos y sedes si aplica.

### People

Necesario para personas, familias, tutores, responsables económicos, autorizados a retirar y contactos.

### Enrollment

Necesario para alumnos, legajo base, matrícula en sección y estado del alumno.

### Academic

Necesario en forma mínima para niveles, salas/grados/años, secciones y turnos si aplica.

No se priorizan inicialmente materias complejas, course offerings avanzados ni asignaciones docentes complejas.

### Attendance

Necesario para sesión de asistencia, registros por alumno, cierre/envío y resumen básico.

### Finance

Necesario para conceptos, cuotas, facturas o cargos, pagos manuales, deuda y estado de cuenta familiar.

### Communication

Necesario para comunicados, destinatarios, notificaciones básicas y acuse o lectura si el tiempo lo permite.

### Files

Necesario en forma básica para documentación del alumno, archivos privados y requisitos documentales.

### Audit

Necesario desde el inicio para creación/edición de alumnos, cambios de tutores, asistencia, pagos, cambios de usuarios, login/logout y soporte platform.

## Criterios de éxito del MVP

El MVP Kindergarten-First se considera validado si permite:

1. Crear un tenant/institución.
2. Configurar Nivel Inicial.
3. Crear salas/secciones.
4. Cargar familias.
5. Cargar alumnos.
6. Vincular tutores y autorizados.
7. Matricular alumnos en salas.
8. Tomar asistencia diaria.
9. Enviar un comunicado básico.
10. Crear conceptos/cuotas.
11. Registrar pagos manuales.
12. Consultar estado de cuenta familiar.
13. Ver un dashboard administrativo básico.
14. Auditar acciones sensibles.
15. Demostrar aislamiento multi-tenant.

## Criterios para habilitar primaria

Primaria podrá entrar al alcance cuando estén estables:

1. Institution.
2. People.
3. Enrollment.
4. Academic structure básica.
5. Attendance.
6. Finance básica.
7. Communication básica.
8. Audit.
9. Tests de aislamiento tenant.
10. RLS en tablas críticas.

Para primaria probablemente se necesite reforzar materias, docentes, carga de notas, períodos, boletines y promoción.

## Criterios para habilitar secundaria

Secundaria podrá entrar al alcance después de primaria o en paralelo solo si el equipo aumenta capacidad.

Para secundaria probablemente se necesite reforzar materias por curso, docentes por materia, evaluaciones, calificaciones, boletines, mesas/recuperatorios, reportes académicos y permisos docentes más finos.

## Riesgos

### Riesgo 1: construir un sistema específico para jardín

Mitigación: mantener nombres internos genéricos y representar jardín como configuración.

### Riesgo 2: postergar demasiado el módulo academic

Mitigación: implementar academic mínimo desde el inicio, aunque la UI de jardín no use todas sus capacidades.

### Riesgo 3: prometer primaria/secundaria antes de tiempo

Mitigación: presentar comercialmente el producto como:

> EduCore SGE — MVP inicial enfocado en Nivel Inicial, con arquitectura preparada para Primaria y Secundaria.

### Riesgo 4: confundir scope de producto con arquitectura

Mitigación: este documento no habilita cambios de arquitectura. Cualquier cambio técnico estructural requiere ADR.

## Regla para Codex

Codex debe tratar este documento como definición de alcance funcional del MVP, no como permiso para modificar arquitectura.

Codex no debe:

- crear entidades específicas para jardín;
- crear endpoints específicos de jardín si existe un recurso general;
- crear paquetes kindergarten;
- cambiar el modelo multi-nivel;
- eliminar soporte conceptual para primaria o secundaria;
- modificar el roadmap sin instrucción explícita.

Codex sí puede:

- priorizar seeds, ejemplos, formularios y labels orientados a Nivel Inicial;
- ocultar en UI funcionalidades no incluidas en el MVP;
- usar `education_levels`, `grade_levels` y `sections` para representar salas;
- mantener nombres internos genéricos.

## Decisión resumida

EduCore SGE será diseñado como sistema escolar multi-nivel desde el día cero, pero el primer MVP se validará con Nivel Inicial/Jardín de Infantes para reducir alcance, acelerar feedback y evitar sobreingeniería inicial.
