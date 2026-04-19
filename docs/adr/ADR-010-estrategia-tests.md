# ADR-010 — Estrategia de Tests: Mantener suite actual, evitar tests triviales

**Fecha:** 2026-04-19
**Estado:** Aceptado
**Sprint:** S2
**Autor:** Equipo Votify + evaluación Claude Sonnet

---

## Contexto

Al finalizar el Sprint 2 la suite de tests cuenta con **147 tests** (73+ unitarios Mockito +
25+ integración @DataJpaTest H2). El equipo preguntó si este número es adecuado o si debería
reducirse.

Se realizó una evaluación técnica clasificando los tests en tres categorías:

| Categoría | Cantidad estimada | Justificación |
|---|---|---|
| Lógica de negocio real | ~120 | POPULAR_VOTE/JURY_EXPERT, periodo activo, auto-voto, comentarios, bulk sum=100, evaluaciones multicriterio |
| Tests de integración JPA | ~20 | Validan queries @Query custom que no se pueden verificar con mocks |
| Tests de framework/triviales | ~7-12 | Verifican que `deleteById` fue llamado, que `findAll` devuelve N elementos, null-ID guards que el framework ya garantiza |

---

## Decisión

**Mantener la suite actual (147 tests). No reducir ahora.**

Adoptar la regla para features futuras: **solo añadir test si cubre una regla de dominio**,
no si cubre comportamiento del framework (JPA, Spring DI, Mockito) o lógica trivial.

---

## Alternativas descartadas

### A) Reducir eliminando los ~12 tests triviales → ~135 tests
- **Descartado porque:** el esfuerzo de identificar y eliminar sin romper coverage supera
  el beneficio (ahorro de ~0.5 s en tiempo de test). El riesgo de eliminar un test que
  parecía trivial pero cubría un edge case real es mayor que mantenerlos.

### B) Aumentar coverage agresivamente (>200 tests)
- **Descartado porque:** el proyecto está en fase académica; tests de alto valor (negocio)
  tienen prioridad sobre coverage estadístico. Los @DataJpaTest son lentos y no escalan
  indefinidamente con H2.

### C) Migrar a Testcontainers para los tests de integración
- **Descartado para Sprint 2:** Testcontainers requiere Docker; el entorno de CI del equipo
  no está configurado para ello. Se puede revisar en Sprint 3 si se añaden queries complejas.

---

## Regla adoptada para nuevas features

> "Escribe un test si y solo si cubre una regla de negocio que pueda romperse sin que el
> compilador o el framework lo detecte."

Casos donde NO es necesario añadir test:
- CRUD básico sin lógica (simples `findById` / `save` sin validaciones propias).
- Null-checks de IDs (el framework lanza `IllegalArgumentException` por diseño).
- Getters/setters de entidades o DTOs.

Casos donde SÍ es necesario:
- Validaciones de negocio (límites, restricciones temporales, unicidad semántica).
- Queries @Query custom con lógica no trivial.
- Ramas de código que manejan casos especiales (re-voto JURY_EXPERT vs POPULAR_VOTE).

---

## Consecuencias

- La suite crece de forma controlada: cada UT de Sprint 3 añadirá tests solo para su
  lógica específica (IA comments, certificados, auditoría).
- El tiempo de `mvn test` se mantiene por debajo de 20 segundos en local.
- Los tests sirven como documentación ejecutable de las reglas de negocio del sistema.
