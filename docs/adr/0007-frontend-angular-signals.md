# ADR-0007 — Frontend Angular standalone y Signals

**Estado:** Aprobado  
**Fecha:** 2026-06-28  
**Decisión:** usar Angular 22 con componentes standalone, Signals y NgRx Signal Store para estado complejo.

---

## Contexto

EduCore SGE necesita interfaces administrativas densas y portales diferenciados por rol. El frontend debe mantener límites por feature, estado predecible, formularios testeables y carga incremental.

## Decisión

- El frontend objetivo será Angular 22.
- Se usarán componentes standalone y Angular Router.
- Las features se cargarán de forma lazy.
- El estado local y derivado usará Signals y `computed`.
- NgRx Signal Store se reservará para features con estado complejo.
- Los formularios usarán Reactive Forms.
- Angular Material/CDK será la base de UI.
- La aplicación se organizará en `core`, `shared`, `layouts` y `features`.
- Vitest cubrirá tests unitarios y Playwright los flujos E2E críticos.
- Los access tokens se mantendrán únicamente en memoria, de acuerdo con ADR-0006.

## Alternativas descartadas

- Módulos NgModule como patrón predeterminado para nuevas features.
- Un store global monolítico para todo el frontend.
- Guardar access tokens en `localStorage` o `sessionStorage`.
- Carga eager de todas las features.

## Consecuencias

- Las features conservan límites y carga independientes.
- Signal Store se introduce solo donde la complejidad lo justifique.
- La arquitectura frontend debe mantener separadas las responsabilidades globales, compartidas, de layout y de negocio.
