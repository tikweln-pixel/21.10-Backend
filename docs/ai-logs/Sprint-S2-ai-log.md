# AI Usage Log — Sprint S2

**Sprint:** S2
**Periodo:** Abril 2026
**Herramientas usadas en este sprint:** Claude Sonnet (Claude Code — modo agente en terminal)

---

## Sesión 1 — 19-04-2026 · UT "Votar con Comentarios"

**Herramienta:** Claude Sonnet (Claude Code, modo plan + implementación)
**UT cubierta:** R8 — "Votar con Comentarios" (Sprint 2), base para
"Sintetizar Comentarios IA" y "Hoja de Ruta de Mejora" (Sprint 3).

---

### 1) Herramientas usadas

- **Claude Sonnet (Claude Code — modo agente):**
  - Exploración en paralelo del backend (`Voting`, `VotingDto`, `VotingService`, tests)
    y del frontend (`Votar.tsx`, `client.ts`, `types.ts`) para localizar el grano correcto.
  - Diseño con agente de planificación: descarte de 3 alternativas y elección de columna
    `comentario TEXT NULL` en `votings`.
  - Implementación completa backend + frontend + ADR-009 + tests.
  - Fix adicional: bloqueo de criterios con `maxPoints = 0` en la UI de votación.

---

### 2) Prompts clave

- **Prompt 1:** `"implementamos en la app votify la funcionalidad comentarios criterios. Permite al
  votante añadir un texto libre junto a su puntuación numérica al votar un proyecto."`
  → Solicitud de implementación completa de comentarios por criterio.

- **Prompt 2:** Respuestas a 3 preguntas de clarificación: comentario por criterio (no por
  proyecto), máximo 500 caracteres, último envío gana / vacío → `NULL`.

- **Prompt 3:** `"revisame los errores de compilacion del backend"` (pegando log de arranque)
  → Diagnóstico: no había errores de compilación — era un conflicto de puerto 8080.

- **Prompt 4:** `"el score (8) excede los puntos máximos (0) — consistencia entre puntuación
  del jurado y slider en votar"`
  → Fix: `maxPoints[cid] || 10` → `maxPoints[cid] ?? 10` + bloqueo UI cuando maxPoints = 0.

---

### 3) Salidas relevantes (resumen corto)

- **Prompt 1 →** La IA exploró ambos repos, diseñó el plan y lo presentó para aprobación.
  Se descartaron: tabla separada `criterion_comments` (redundante con unicidad ya en `Voting`),
  reusar `Comment` con `criterion_id` nullable (mezcla alcances), reusar `EvaluacionComentario`
  (flujo de jurado/experto, no votante numérico). Se eligió columna `comentario TEXT NULL` en
  `votings` por ser el grano natural ya garantizado por `Voting` (ver ADR-009).

- **Prompts 2-3 →** Implementación backend: campo en `Voting` + `VotingDto`, helper
  `normalizeComment()` con validación de longitud, lógica en `create()` (rama nuevo y merge)
  y `update()` (con null guard). Frontend: `critComments` por criterio en `Votar.tsx`,
  textarea + contador bajo cada slider, envío del `comentario` en `createVoting()`.

- **Prompt 4 →** Bug detectado en UI: `0 || 10` en JS devuelve `10` cuando `maxPoints = 0`.
  Fix: cambio a `0 ?? 10`, skip del criterio en el loop de `handleVote()`, y estado visual
  bloqueado ("Sin puntos") cuando el máximo configurado es 0.

---

### 4) Qué aceptamos y qué rechazamos

- **Aceptado:** Columna `comentario TEXT NULL` en `votings` (Opción A del ADR-009) → por qué:
  unicidad `(voter, competitor, criterion, category)` ya garantizada en `Voting`; score y
  comentario viajan juntos en la misma escritura; query trivial para IA de Sprint 3.

- **Aceptado:** Semántica "último gana, vacío = NULL" → por qué: coherente con cómo se
  sobrescribe `score` en JURY_EXPERT; no genera acumulación incontrolada.

- **Aceptado:** `normalizeComment()` con `trimmed.length() > 500` → RuntimeException →
  validación en el servicio, no en el controlador; máximo aplicado también con `maxLength`
  en el frontend.

- **Aceptado:** Null guard en `update()` (`if (dto.getComentario() != null)`) → por qué:
  los PUTs administrativos de `IntervencionManual.tsx` no envían `comentario`; sin null guard
  borrarían el comentario existente sin intención.

- **Rechazado:** Tabla separada `criterion_comments` → redundante: `Voting` ya tiene la
  unicidad correcta; añadiría JOIN en cada lectura sin beneficio.

- **Rechazado:** Reusar `Comment` con `criterion_id` nullable → rompe la invariante de la
  vista "comentarios recibidos por competidor" que consume `Comment` a nivel proyecto.

---

### 5) Cómo lo verificamos

- [X] `mvn test` — 140 tests en verde (98 previos + 6 nuevos de comentarios).
- [X] TypeScript: `tsc --noEmit` sin errores en los ficheros tocados.
- [ ] Arrancar backend local y confirmar que Hibernate emite `ALTER TABLE votings ADD COLUMN comentario TEXT`.
- [ ] Verificar en Supabase SQL: `SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'votings' AND column_name = 'comentario';`
- [ ] Flujo manual: seleccionar evento + categoría, escribir comentarios por criterio, enviar voto, verificar en BD.
- [ ] Re-voto: cambiar comentario de un criterio → la fila no se duplica, el comentario queda actualizado.
- [ ] Vaciar comentario → `comentario = NULL` en BD.
- [ ] Criterio con `maxPoints = 0` → slider bloqueado, no se envía voto para ese criterio.

---

### 6) Resultado final / decisión humana

Se implementó la UT "Votar con Comentarios" con comentario por criterio almacenado en
la tabla `votings`. La decisión de arquitectura queda documentada en ADR-009.

Se detectó y corrigió un bug secundario: los criterios con `maxPoints = 0` configurados
por el jurado no bloqueaban el slider (JavaScript trata `0` como falsy en `|| 10`),
lo que provocaba que el backend rechazara el voto con error de validación.

**Ficheros modificados:**
- Backend: `Voting.java`, `VotingDto.java`, `VotingService.java`,
  `VotingServiceTest.java`, `VotingRepositoryTest.java`
- Frontend: `Votar.tsx`, `client.ts`, `types.ts`
- Documentación: `docs/adr/ADR-009-comentarios-por-criterio-en-voting.md` (nuevo)

**Revisión humana pendiente:**
- Confirmar que Hibernate emite el ALTER TABLE en el primer arranque tras el cambio.
- Validar que 500 caracteres es suficiente en pruebas reales con usuarios.
- Decidir en un futuro ADR qué hacer con la entidad `Comment` proyecto-level
  (mantener, desusar, reorientar).

**Referencias en el repo:**
- `docs/adr/ADR-009-comentarios-por-criterio-en-voting.md`
- `src/main/java/com/votify/entity/Voting.java`
- `src/main/java/com/votify/dto/VotingDto.java`
- `src/main/java/com/votify/service/VotingService.java`
- `src/test/java/com/votify/service/VotingServiceTest.java`
- `src/test/java/com/votify/persistence/VotingRepositoryTest.java`
- Frontend: `src/pages/Votar.tsx`, `src/api/client.ts`, `src/types.ts`

---

## Sesión 2 — 19-04-2026 · UT "Control de Votos" + Evaluación Estrategia de Tests

**Herramienta:** Claude Sonnet (Claude Code, modo agente)
**UT cubierta:** R14/R17 — "Control de Votos" (Sprint 2).

---

### 1) Herramientas usadas

- **Claude Sonnet (Claude Code — modo agente):**
  - Exploración de `VotingService.java`, `VotingServiceTest.java`, `Category.java`,
    `CategoryCriterionPoints.java` para entender el estado actual.
  - Implementación de validaciones en `VotingService.create()`: auto-voto y periodo inactivo.
  - Adición de 7 tests de aceptación para "Control de Votos".
  - Evaluación crítica de la estrategia de tests del proyecto (147 tests totales).

---

### 2) Prompts clave

- **Prompt 1:** `"hay que aplicar pruebas de aceptacion a ut control de votos"` + 7 PAs descritas
  manualmente (periodo cerrado, periodo activo, re-voto, auto-voto, bloqueo en tiempo real,
  intervención manual, usuario sin rol votante).

- **Prompt 2:** `"Evaluame como programador senior y añade posterior a la evaluacion este promt
  a ia usage y adr sprint2, evaluame la necesidad de los 100 y pico test en nuestro proyecto,
  no cabria la posibilidad de reducir su numero?"`
  → Evaluación crítica: 147 tests mayormente justificados; ~12-15 triviales son candidatos a
  eliminar pero el esfuerzo no justifica el ahorro. Recomendación: no reducir ahora.

---

### 3) Salidas relevantes

- **Prompt 1 →** Se añadieron dos validaciones a `VotingService.create()`:
  - **Auto-voto:** `if (voter.getId().equals(competitor.getId())) throw`
  - **Periodo inactivo:** carga anticipada de `Category` con `isPeriodActive()` basado en
    `cat.getTimeInitial() / cat.getTimeFinal()` (java.util.Date).
  - La carga de `Category` se unificó (antes se cargaba dos veces en ramas separadas).
  - 7 nuevos tests unitarios (Mockito): re-voto JURY_EXPERT, periodo cerrado, periodo no
    iniciado, periodo activo (happy path), bloqueo en tiempo real, intervención manual
    supervisor, auto-voto.
  - Total tests: 147 (anterior: 140). VotingServiceTest: 27 tests (anterior: 20).

- **Prompt 2 →** Evaluación senior de la suite de tests:
  - Justificados: tests de lógica de negocio (POPULAR_VOTE, JURY_EXPERT, periodo, auto-voto,
    comentarios, bulk validación). Tests de integración JPA (@DataJpaTest).
  - Candidatos a eliminar (~12-15): tests de tipo "verifica que deleteById fue llamado",
    "findAll devuelve 2 elementos", null-ID guards que el framework ya garantiza.
  - Decisión documentada en ADR-010.

---

### 4) Qué aceptamos y qué rechazamos

- **Aceptado:** Carga anticipada de `Category` en `create()` (elimina carga duplicada en dos
  ramas) → refactoring mínimo con beneficio de claridad + habilita el check de periodo.

- **Aceptado:** `isPeriodActive()` como método privado en el servicio usando
  `System.currentTimeMillis()` → simple, testeable con mocks de `Category`.

- **Aceptado:** Auto-voto check a nivel de IDs (`voter.getId().equals(competitor.getId())`)
  antes de cargar el criterio → falla rápido, sin carga innecesaria de repositorios.

- **Aceptado (evaluación tests):** No reducir la suite ahora. Mantener 147 tests.
  Regla adoptada: futuros tests solo si cubren lógica de dominio, no comportamiento de framework.

- **Rechazado:** ADR separado por "cada sprint" — los ADRs son por decisión arquitectónica,
  no por sprint. Se creó ADR-010 específico para estrategia de tests.

---

### 5) Cómo lo verificamos

- [X] `mvn test` — 147 tests en verde (140 previos + 7 nuevos de Control de Votos).
- [ ] Flujo manual: intentar votar con periodo cerrado → backend responde 500 con mensaje claro.
- [ ] Flujo manual: votante con mismo userId que competidor → backend rechaza.
- [ ] Flujo manual: votar durante periodo activo → funciona correctamente.

---

### 6) Resultado final / decisión humana

Se implementaron las validaciones de "Control de Votos" en `VotingService`:
período activo e impedimento de auto-voto. 7 pruebas de aceptación en verde.

Se realizó evaluación técnica de la suite de tests: 147 tests son mayormente justificados.
No se recomienda reducir ahora; la regla adoptada es evitar tests triviales en nuevas features.

**Ficheros modificados:**
- Backend: `VotingService.java`, `VotingServiceTest.java`
- Documentación: `docs/adr/ADR-010-estrategia-tests.md` (nuevo), `docs/ai-logs/Sprint-S2-ai-log.md`

**Revisión humana pendiente:**
- Confirmar que el mensaje de error "periodo de votación no activo" es suficientemente claro
  para el usuario final en la UI (Evaluar.tsx, Votar.tsx ya muestran el `err.response.data.message`).
- Decidir si el auto-voto debe bloquearse también en la UI antes de llegar al backend.

**Referencias en el repo:**
- `src/main/java/com/votify/service/VotingService.java`
- `src/test/java/com/votify/service/VotingServiceTest.java`
- `docs/adr/ADR-010-estrategia-tests.md`
