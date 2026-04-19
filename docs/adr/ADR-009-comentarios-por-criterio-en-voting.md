# ADR-009: Comentarios por criterio viven en la fila de `Voting`

**Fecha:** 19-04-2026
**Sprint:** S2
**Estado:** Aprobado

---

### 1) Contexto

La UT **"Votar con Comentarios"** del Sprint 2 (R8) pide que el votante pueda
adjuntar un texto libre por cada criterio junto a su puntuación numérica al
votar un proyecto. Estos comentarios son el input de dos features de IA
previstas en Sprint 3:

- **UT "Sintetizar Comentarios IA"**: resume automáticamente los comentarios
  recibidos por cada competidor.
- **Hoja de Ruta de Mejora** (`Hoja_ruta_mejora`): genera recomendaciones de
  mejora por competidor a partir de los comentarios agregados.

Antes de esta UT, la pantalla `/votar` solo recogía **un comentario por
proyecto** (entidad `Comment` con relación `project ↔ voter`). Esta
granularidad es demasiado gruesa: la IA no puede atribuir un comentario a un
criterio concreto ni a un competidor concreto cuando un proyecto tiene varios.

La entidad `Voting` ya es única por la combinación
`(voter, competitor, criterion, category)` — cada fila identifica exactamente
"qué votante puntuó qué competidor en qué criterio en qué categoría". Es el
grano natural donde colocar el comentario.

---

### 2) Opciones consideradas

- **Opción A — Columna `comentario` en `votings` (elegida):** añadir un campo
  `String comentario` (`TEXT NULL`) a la entidad `Voting`, junto al `score`.
- **Opción B — Nueva tabla `criterion_comments`:** tabla separada con clave
  `(voter_id, competitor_id, criterion_id)`.
- **Opción C — Reusar `Comment` con `criterion_id` nullable:** añadir
  `criterion_id` opcional a la entidad `Comment` existente.
- **Opción D — Reusar `EvaluacionComentario` (jerarquía `Evaluacion`):** crear
  evaluaciones de tipo `COMENTARIO` para cada (votante, competidor, criterio).

---

### 3) Criterios de decisión

- Unicidad ya garantizada por `Voting` — no duplicar restricciones.
- Mínima latencia en el flujo de voto: una sola escritura por criterio, no dos.
- Claridad semántica: score y justificación viajan juntos.
- Facilidad para la futura query de agregación por competidor que
  consumirán los resumenes de comentarios con IA de S2.
- Backwards-compatible: votos previos siguen válidos (columna nullable).

---

### 4) Decisión

**Elegimos la Opción A.** Añadimos `comentario TEXT NULL` a `votings` /
`Voting`, con longitud máxima de 500 caracteres validada en
`VotingService.normalizeComment()`. El comentario se normaliza (trim, vacío →
`NULL`) y la semántica en re-voto es "último gana" (coherente con cómo se
sobrescribe `score` en JURY_EXPERT).

- Opción B descartada por redundante: `Voting` ya impone
  `UNIQUE(voter, competitor, criterion)` — una tabla paralela repetiría la
  misma clave y forzaría un JOIN en cada lectura.
- Opción C descartada por mezclar alcances: `Comment` es proyecto-level y lo
  consume la vista "comentarios recibidos por competidor" desde otro ángulo.
  Añadir `criterion_id` nullable rompería la invariante de esa vista.
- Opción D descartada por pertenencia incorrecta: `EvaluacionComentario`
  pertenece al flujo cualitativo de jurado/experto (con score = null), no al
  flujo numérico del votante.

---

### 5) Consecuencias

**Positivas:**

- Sprint 3 puede exponer
  `GET /api/votings/comments?competitorId=...` como query trivial sobre la
  tabla existente — cero reestructuración.
- La Hoja de Ruta de Mejora recibe comentarios ya atribuidos a criterios, lo
  que permite al LLM agrupar por dimensión sin prompt engineering adicional.
- `ddl-auto=update` crea la columna en Supabase sin migración manual.

**Negativas / trade-offs aceptados:**

- Si en el futuro se quisiera un comentario sin score (comentario puro), habría
  que replantear el modelo. La UT actual ata comentario a score, así que es
  aceptable para S1 y S3.
- El campo `Comment` proyecto-level queda sin uso desde `/votar` (lo sigue
  usando la vista de "comentarios recibidos"). Se desusará gradualmente o se
  reorientará en un futuro ADR.

---

### 6) Validación

- Hibernate con `ddl-auto=update` emite
  `ALTER TABLE votings ADD COLUMN comentario TEXT` al primer arranque.
- Tests añadidos en `VotingServiceTest` (5 nuevos) y `VotingRepositoryTest` (1
  nuevo) — todos en verde.
- Frontend (`Votar.tsx`) renderiza un `<textarea maxLength=500>` bajo cada
  slider de criterio y envía el comentario en el mismo `POST /api/votings`.
