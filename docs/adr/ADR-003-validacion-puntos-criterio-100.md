# ADR-003: Estrategia de validación de puntos por criterio (suma = 100) en CategoryService

**Fecha:** 21-03-2026
**Sprint:** S0
**Estado:** Aprobado

---

### 1) Contexto

El requisito funcional RF-04 (Configurar Puntos) exige que el organizador pueda asignar puntos máximos a cada criterio de evaluación dentro de una categoría, con la restricción de que la suma total de todos los criterios de una categoría sea exactamente 100 puntos.

La UI presenta esta configuración como un conjunto de sliders (uno por criterio). El organizador puede ajustar un criterio individualmente o guardar todos a la vez pulsando "Aceptar". Hay que decidir cómo validar y persistir estos puntos a nivel de servicio.

### 2) Opciones consideradas

- **Opción A:** Validar solo en el guardado masivo ("Aceptar") — un único endpoint bulk que recibe todos los criterios y valida que sumen 100 antes de guardar.
- **Opción B:** Validar en cada actualización individual — al mover un slider, el servidor comprueba que el valor propuesto no exceda el margen disponible (100 - suma del resto).
- **Opción C:** Híbrido — dos endpoints: uno individual (permite parciales, solo valida que el acumulado ≤ 100) y uno bulk (exige suma exacta = 100). La UI usa el individual para feedback en tiempo real y el bulk al confirmar.

### 3) Criterios de decisión

- Experiencia de usuario: el organizador debe recibir feedback inmediato si un slider sobrepasa el límite.
- Integridad de datos: la BD no debe quedar en un estado donde la suma ≠ 100.
- Testabilidad: los dos comportamientos deben poder testearse de forma independiente.
- Simplicidad de implementación en el Sprint 0.

### 4) Decisión tomada

Se elige la **Opción C (híbrido)**, implementada en `CategoryService` con dos métodos:

1. **`setCriterionPoints(categoryId, criterionId, maxPoints)`** — actualiza un criterio individual. Valida que `suma_del_resto + maxPoints ≤ 100`. Permite estados intermedios donde la suma total sea inferior a 100 (el organizador está en medio de la configuración). Implementa upsert: si ya existe el registro para ese par (categoría, criterio), lo actualiza; si no, lo crea.

2. **`setCriterionPointsBulk(categoryId, pointsDtos)`** — reemplaza toda la configuración de puntos de una categoría. Exige que la suma de todos los `maxPoints` sea exactamente 100. Borra los registros anteriores (`deleteByCategoryId`) y guarda los nuevos en una sola transacción (`@Transactional`).

### 5) Consecuencias

- **Positivas:**
  - El endpoint individual permite al frontend dar feedback en tiempo real mientras el usuario mueve sliders.
  - El endpoint bulk garantiza la integridad final: la BD solo queda con configuraciones válidas (suma = 100) tras confirmar.
  - La lógica de validación está centralizada en el servicio, no en el frontend.
  - Ambos casos tienen tests unitarios independientes (`CategoryServiceTest`, tests 10-13).

- **Negativas / trade-offs:**
  - Dos endpoints para la misma entidad introduce complejidad en la API (`PUT /categories/{id}/criterion-points/{criterionId}` vs `PUT /categories/{id}/criterion-points`).
  - El estado intermedio (suma < 100) es válido en la BD si el organizador no ha confirmado. Si el sistema se cae antes de "Aceptar", los datos quedan inconsistentes.

- **Riesgos y mitigaciones:**
  - Riesgo: el frontend podría omitir llamar al endpoint bulk y dejar la BD con suma ≠ 100. Mitigación: añadir en un sprint futuro una validación antes de abrir el periodo de votación que compruebe que la suma = 100 en todas las categorías con criterios configurados.

### 6) Evidencia

- `CategoryService.java`: métodos `setCriterionPoints` (L144-175) y `setCriterionPointsBulk` (L183-215).
- `CategoryServiceTest.java`: tests 10-13 (bulk válido, puntos negativos, suma ≠ 100, individual > 100).
- Commit: `16031ad Configurar criterios de evaluacion: CategoryService hecho`.
