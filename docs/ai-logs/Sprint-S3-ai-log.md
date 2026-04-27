# AI Usage Log — Sprint S3

**Sprint:** S3
**Periodo:** Abril 2026 →
**Herramientas usadas en este sprint:** Claude Sonnet (Cowork mode — agente con acceso a ficheros)

---

## Sesión 1 — 27-04-2026 · UT "Hoja de Ruta de Mejora" (implementación parcial)

**Herramienta:** Claude Sonnet (Cowork mode, acceso a `21.03-Backend`)
**UT cubierta:** "Sintetizar Comentarios IA" / "Hoja de Ruta de Mejora" (Sprint 3, implementación parcial sin LLM).

---

### 1) Herramientas usadas

- **Claude Sonnet (Cowork mode — agente):**
  - Exploración de `EvaluacionComentario`, `Evaluacion`, `EvaluacionRepository`,
    `CompetitorController`, `CommentRepository`, `Criterion` y DTOs existentes
    para entender qué datos están disponibles y dónde encaja la nueva feature.
  - Diseño de la arquitectura con cuatro opciones consideradas (ver ADR-012).
  - Implementación completa backend: entity, repository, 3 DTOs, service, endpoints.
  - Explicación del plan de integración futura de IA (Anthropic API, Opción A vs Spring AI).
  - Generación de dos ficheros frontend (`downloadHojaRutaPdf.ts`, `useHojaRuta.ts`)
    para descarga PDF desde el cliente con `jsPDF`.
  - Creación de ADR-012 y este log.

---

### 2) Prompts clave

- **Prompt 1:** _(imagen del mockup Figma con "Hoja de Ruta Mejorada" y "Dashboard Participantes")_
  `"relativo a la tarea IA para generar una hoja de ruta de mejora personalizada para cada competidor,
  sintetizando los comentarios que recibió de los expertos. [...] vamos a implementar la
  funcionalidad parcial de hoja de ruta"`
  → Exploración del codebase y diseño completo de la feature.

- **Prompt 2:** `"la hoja de ruta mejora se deberia poder descargar"`
  → Decisión de generar el PDF en el frontend con `jsPDF` (sin endpoint PDF en backend).

- **Prompt 3:** `"enseñame el plan antes de implementar"` _(referente a la descarga PDF)_
  → Presentación del plan: `jsPDF`, `downloadHojaRutaPdf.ts`, endpoint
  `GET /hoja-ruta/pdf` descartado a favor de generación en cliente.

- **Prompt 4:** `"no generamos en el backend, si me parece bien. aunque antes explicame
  como añadir IA para que resuma los comentarios?"`
  → Explicación de Opción A (Java 21 `HttpClient` + `AnthropicClient`) vs Opción B
  (Spring AI). Recomendación: Opción A para el sprint actual.

- **Prompt 5:** `"1 descarga pdf desde el frontend"`
  → Implementación de `downloadHojaRutaPdf.ts` y `useHojaRuta.ts`.

- **Prompt 6:** `"ten en cuenta actualizar en doc adrs, ia logs"`
  → ADR-012 y este fichero.

---

### 3) Salidas relevantes

- **Prompts 1-2 →** Exploración del codebase. Identificado que los comentarios
  de expertos viven en `EvaluacionComentario` (campo `datos: {"texto":"..."}`,
  con `criterion`, `competitor`, `category`, `evaluador`). Decisión de consumir
  exclusivamente `EvaluacionComentario` para la hoja de ruta (no `votings.comentario`
  que pertenece al flujo de votante, según ADR-009).

  Archivos creados en backend:
  - `entity/HojaRutaMejora.java` — `UNIQUE(competitor_id, category_id)`, nullable `category_id`
  - `persistence/HojaRutaMejoraRepository.java` — queries por competitor + category (nullable)
  - `dto/HojaRutaMejoraDto.java`, `dto/AreaMejoraDto.java`, `dto/ComentarioExpertoDto.java`
  - `service/HojaRutaMejoraService.java` — `getOrGenerar()`, `generar()`,
    `buildAreasMejora()` (agrupa por criterio), `buildResumenAutomatico()` (sin IA),
    `extraerTexto()` (parsing manual de `{"texto":"..."}`)
  - `controller/CompetitorController.java` — añadidos `GET /{id}/hoja-ruta` y
    `POST /{id}/hoja-ruta/generar`; inyectado `HojaRutaMejoraService`.

- **Prompt 3 →** Plan PDF presentado y aprobado: generación en cliente, sin nuevo
  endpoint en backend.

- **Prompt 4 →** Explicación de la ruta de integración de IA:
  - `AnthropicClient.java` con `java.net.http.HttpClient` (Java 21 nativo).
  - Cambio quirúrgico: solo reemplazar `buildResumenAutomatico()` + setear `generadoIa = true`.
  - Spring AI descartado para este sprint por ser más nuevo y añadir complejidad.

- **Prompt 5 →** Implementación frontend:
  - `src/utils/downloadHojaRutaPdf.ts` — genera PDF con `jsPDF`: encabezado indigo,
    resumen, áreas de mejora por criterio con comentarios por experto, paginación.
  - `src/hooks/useHojaRuta.ts` — `cargar()`, `regenerar()`, `descargarPdf()`.

- **Prompt 6 →** Este ADR-012 y este log.

---

### 4) Qué aceptamos y qué rechazamos

- **Aceptado:** Consumir `EvaluacionComentario` (no `votings.comentario`) → por qué:
  los comentarios de expertos ya tienen `criterion` + `evaluador` identificados,
  que son los campos que dan valor analítico a la hoja de ruta.

- **Aceptado:** Persistir solo metadatos en `HojaRutaMejora` (no el detalle por criterio)
  → por qué: el detalle ya existe en `evaluaciones`; desnormalizarlo crearía riesgo de
  desincronización si un experto edita su evaluación.

- **Aceptado:** Parsing manual de `{"texto":"..."}` con `indexOf` → por qué: el formato
  del campo `datos` es fijo y documentado en el JavaDoc de `EvaluacionComentario`;
  evita inyectar `ObjectMapper` en el service por un único caso de uso.

- **Aceptado:** `category_id` nullable en `HojaRutaMejora` → por qué: permite hojas
  de ruta globales (todas las categorías); el NULL-safe se gestiona con queries
  específicas (`findByCompetitorIdAndCategoryIsNull`).

- **Aceptado:** PDF generado en el frontend con `jsPDF` → por qué: evita añadir
  una librería PDF al backend (iText/OpenPDF), la generación es síncrona en el
  cliente y no consume recursos del servidor.

- **Rechazado:** Integración de Anthropic API en este sprint → la UT exige
  "implementación parcial"; la IA se integra en una sesión futura siguiendo el patrón
  documentado en ADR-012 §5.

- **Rechazado:** Spring AI como abstracción del LLM → añade dependencias nuevas sin
  beneficio claro en este sprint; se reevalúa en Sprint 4 si hay más features de IA.

- **Rechazado:** Endpoint `GET /hoja-ruta/pdf` en backend → la generación PDF en el
  cliente es suficiente y más simple; no requiere serializar el PDF en el servidor.

---

### 5) Cómo lo verificamos

- [ ] Hibernate emite `CREATE TABLE hoja_ruta_mejora` al primer arranque
      con `ddl-auto=update`.
- [ ] `GET /api/competitors/{id}/hoja-ruta` con competidor sin evaluaciones
      → 200 con `areasMejora: []` y resumen "Aún no hay comentarios".
- [ ] `GET /api/competitors/{id}/hoja-ruta` con `EvaluacionComentario` existentes
      → áreas agrupadas por criterio, texto extraído correctamente.
- [ ] `POST /api/competitors/{id}/hoja-ruta/generar` → sobrescribe sin duplicar filas.
- [ ] `GET /api/competitors/{id}/hoja-ruta?categoryId=X` → filtra por categoría.
- [ ] Frontend: botón "Descargar PDF" genera `hoja-ruta-{nombre}.pdf` descargable.
- [ ] `generadoIa: false` en todas las respuestas.
- [ ] TypeScript: `tsc --noEmit` sin errores en `downloadHojaRutaPdf.ts` y `useHojaRuta.ts`.

---

### 6) Resultado final / decisión humana

Se implementó la funcionalidad parcial de Hoja de Ruta de Mejora: backend completo
(entity → repository → service → controller) y frontend (utilidad PDF + hook).
La integración de IA real queda documentada como extensión de un único método
(`buildResumenAutomatico`) con un nuevo componente (`AnthropicClient`).

**Deuda técnica registrada:**
- Integrar `AnthropicClient` + `ANTHROPIC_API_KEY` en Railway cuando se active la
  feature de IA (ADR-012 §5).
- Añadir tests unitarios para `HojaRutaMejoraService` (Mockito) y de integración
  para `HojaRutaMejoraRepository` (@DataJpaTest H2).
- Decidir si `category_id = NULL` (hoja global) es un caso de uso real o se elimina
  la nullable para simplificar el modelo.

**Ficheros creados/modificados:**
- Backend (nuevos): `HojaRutaMejora.java`, `HojaRutaMejoraRepository.java`,
  `HojaRutaMejoraDto.java`, `AreaMejoraDto.java`, `ComentarioExpertoDto.java`,
  `HojaRutaMejoraService.java`
- Backend (modificado): `CompetitorController.java`
- Frontend (nuevos): `src/utils/downloadHojaRutaPdf.ts`, `src/hooks/useHojaRuta.ts`
- Documentación: `docs/adr/ADR-012-hoja-ruta-mejora-implementacion-parcial.md` (nuevo),
  `docs/ai-logs/Sprint-S3-ai-log.md` (este fichero)

**Referencias en el repo:**
- `src/main/java/com/votify/entity/HojaRutaMejora.java`
- `src/main/java/com/votify/persistence/HojaRutaMejoraRepository.java`
- `src/main/java/com/votify/service/HojaRutaMejoraService.java`
- `src/main/java/com/votify/controller/CompetitorController.java`
- `docs/adr/ADR-012-hoja-ruta-mejora-implementacion-parcial.md`
