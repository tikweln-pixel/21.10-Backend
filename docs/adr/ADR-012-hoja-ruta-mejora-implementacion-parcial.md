# ADR-012: Hoja de Ruta de Mejora — implementación parcial sin IA

**Fecha:** 27-04-2026
**Sprint:** S3
**Estado:** Aprobado

---

### 1) Contexto

La UT **"Sintetizar Comentarios IA"** del Sprint 3 plantea generar una
**Hoja de Ruta de Mejora** personalizada para cada competidor, sintetizando
los comentarios de expertos que recibió durante la competición.

Los comentarios cualitativos de expertos ya existen en la entidad
`EvaluacionComentario` (campo `datos` con JSON `{"texto":"..."}`), creada en
la jerarquía Factory Method de evaluaciones (ADR-006). Cada evaluación de tipo
`COMENTARIO` lleva asociado un `criterion`, un `competitor`, un `category` y
un `evaluador`, lo que permite agrupar los comentarios por criterio para la
hoja de ruta.

El modelo de dominio define `Hoja_ruta_mejora (id, competidor_id,
resumen_generado, generado_ia)`. El requisito de sprint es implementar
**la funcionalidad parcial**: sin llamada a un LLM externo, pero con la
estructura lista para que la integración de IA sea un cambio mínimo futuro.

El ADR-009 dejó documentado que los comentarios de votantes van en `votings.comentario`
y los de expertos en `EvaluacionComentario`. La hoja de ruta consume exclusivamente
los de expertos (`EvaluacionComentario`) porque son los que tienen criterio
y evaluador identificados.

---

### 2) Opciones consideradas

- **Opción A — Implementación parcial con generación automática (elegida):**
  Agrupa los `EvaluacionComentario` por criterio, genera un resumen automático
  de texto (sin LLM) y lo persiste en una nueva tabla `hoja_ruta_mejora`.
  El campo `generado_ia = false`. La integración de IA en el futuro solo
  requiere reemplazar el método `buildResumenAutomatico()` y setear
  `generado_ia = true`.

- **Opción B — Generación completa con IA desde el primer momento:**
  Integrar la API de Anthropic (Claude) en el backend en este sprint.
  Requeriría añadir `ANTHROPIC_API_KEY` a Railway, gestionar errores de red
  en producción y consumo de tokens. Aumenta la complejidad de este sprint
  sin que el requisito lo exija.

- **Opción C — Sin persistencia, solo derivado en tiempo real:**
  No crear tabla `hoja_ruta_mejora`; el endpoint genera la estructura
  al vuelo desde `EvaluacionComentario` en cada petición y nunca persiste.
  Más simple pero impide guardar versiones, fechar la generación o marcar
  `generado_ia` cuando se integre la IA.

- **Opción D — Persistir el contenido completo como JSON blob:**
  Guardar la lista de áreas de mejora serializada en un campo `TEXT` de la
  entidad. Desnormaliza los comentarios que ya existen en `evaluaciones` y
  crea riesgo de datos desincronizados.

---

### 3) Criterios de decisión

- Cumplir el requisito de sprint sin añadir deuda técnica arquitectónica.
- La estructura debe ser extensible para IA con el mínimo cambio posible.
- No desnormalizar datos ya existentes en `evaluaciones`.
- El competidor debe poder descargar la hoja de ruta en PDF (feature
  requerida en la misma sesión).

---

### 4) Decisión

**Elegimos la Opción A.** La entidad `HojaRutaMejora` persiste solo los
metadatos de la generación (`competitor`, `category`, `resumenGenerado`,
`generadoIa`, `fechaGeneracion`). El detalle por criterio (áreas de mejora)
se deriva siempre en tiempo real desde `EvaluacionComentario` en
`HojaRutaMejoraService.buildAreasMejora()`, sin desnormalización.

- Opción B descartada: la integración de IA queda documentada como extensión
  futura con el patrón exacto de cambio (ver sección 5).
- Opción C descartada: sin persistencia no es posible fechar la generación
  ni marcar `generado_ia`; tampoco se cumpliría el modelo de dominio definido.
- Opción D descartada: desnormaliza comentarios ya almacenados en
  `evaluaciones`; riesgo de datos desincronizados si un experto edita su
  evaluación.

La restricción de unicidad `UNIQUE(competitor_id, category_id)` garantiza que
solo existe una hoja de ruta activa por competidor y categoría. El endpoint
`POST /hoja-ruta/generar` borra la anterior antes de persistir la nueva.

El campo `category_id` es nullable para permitir hojas de ruta globales
(todas las categorías del competidor), aunque en la implementación actual
el frontend la solicita siempre con `categoryId` concreto.

---

### 5) Extensión futura con IA

Cuando se integre la API de Anthropic (recomendado en Sprint 3 avanzado o
Sprint 4), el cambio se limita a:

1. Crear `AnthropicClient.java` (`@Component`) que llama a
   `POST https://api.anthropic.com/v1/messages` usando `java.net.http.HttpClient`
   (incluido en Java 21, sin dependencias nuevas).
2. Reemplazar el cuerpo de `HojaRutaMejoraService.buildResumenAutomatico()`
   por una llamada a `anthropicClient.resumir(prompt)`.
3. Setear `generadoIa = true` en la entidad antes de persistir.
4. Añadir `ANTHROPIC_API_KEY` como variable de entorno en Railway.

Ningún otro fichero cambia (entity, repository, DTOs, controller, frontend).

---

### 6) Estructura de clases resultante

```
entity/
  HojaRutaMejora          — id, competitor, category, resumenGenerado,
                            generadoIa, fechaGeneracion
persistence/
  HojaRutaMejoraRepository — findByCompetitorId,
                              findByCompetitorIdAndCategoryId,
                              findByCompetitorIdAndCategoryIsNull
dto/
  HojaRutaMejoraDto        — id, competitorId, categoryId, resumenGeneral,
                              List<AreaMejoraDto>, generadoIa, fechaGeneracion
  AreaMejoraDto            — criterioId, criterioNombre,
                              List<ComentarioExpertoDto>
  ComentarioExpertoDto     — evaluadorId, evaluadorNombre, texto
service/
  HojaRutaMejoraService    — getOrGenerar(), generar(),
                              buildAreasMejora(), buildResumenAutomatico()
controller/
  CompetitorController     — GET /{id}/hoja-ruta
                             POST /{id}/hoja-ruta/generar
```

**Frontend (repo `votify-frontend`):**
```
src/utils/downloadHojaRutaPdf.ts  — jsPDF, sin llamada al backend
src/hooks/useHojaRuta.ts          — cargar(), regenerar(), descargarPdf()
```

---

### 7) Consecuencias

**Positivas:**
- Sprint 3 cubre la UT sin introducir dependencias externas (sin SDK de IA).
- La integración de IA real es un cambio quirúrgico de un método y una clase.
- La descarga PDF funciona desde el frontend con `jsPDF`, sin endpoint
  adicional en el backend y sin depender de `html2canvas`.
- El modelo de dominio queda implementado fielmente
  (`hoja_ruta_mejora` con `generado_ia`).

**Negativas / trade-offs aceptados:**
- El `resumenGeneral` automático es informativo pero no añade valor analítico;
  la deuda de valor se salda cuando se integre la IA.
- La extracción del campo `texto` del JSON `{"texto":"..."}` usa parsing
  manual (indexOf) en lugar de Jackson. Acceptable porque el formato es fijo
  y evita inyectar `ObjectMapper` en el service solo para este caso.
- El campo `category_id` nullable en la restricción UNIQUE puede causar
  problemas en PostgreSQL si se insertan dos hojas sin categoría (NULL ≠ NULL
  en SQL). La query `findByCompetitorIdAndCategoryIsNull` + el método
  `deleteByCompetitorIdAndCategoryIsNull` garantizan que nunca haya dos filas
  con `category_id = NULL` para el mismo competitor.

---

### 8) Validación

- [ ] Hibernate emite `CREATE TABLE hoja_ruta_mejora` al primer arranque
      (o `ALTER TABLE` si la BD ya existe con `ddl-auto=update`).
- [ ] `GET /api/competitors/{id}/hoja-ruta` con competidor sin evaluaciones
      devuelve 200 con `areasMejora: []` y mensaje "Aún no hay comentarios".
- [ ] `GET /api/competitors/{id}/hoja-ruta` con evaluaciones existentes
      devuelve las áreas agrupadas por criterio con el texto del comentario.
- [ ] `POST /api/competitors/{id}/hoja-ruta/generar` regenera y sobrescribe
      la hoja anterior (la tabla no acumula duplicados).
- [ ] Frontend: botón "Descargar PDF" genera el archivo
      `hoja-ruta-{nombre}.pdf` con el contenido correcto.
- [ ] `generadoIa: false` en todas las respuestas de la implementación parcial.

---

### 9) Referencias

- `src/main/java/com/votify/entity/HojaRutaMejora.java`
- `src/main/java/com/votify/persistence/HojaRutaMejoraRepository.java`
- `src/main/java/com/votify/dto/HojaRutaMejoraDto.java`
- `src/main/java/com/votify/dto/AreaMejoraDto.java`
- `src/main/java/com/votify/dto/ComentarioExpertoDto.java`
- `src/main/java/com/votify/service/HojaRutaMejoraService.java`
- `src/main/java/com/votify/controller/CompetitorController.java`
- Frontend: `src/utils/downloadHojaRutaPdf.ts`, `src/hooks/useHojaRuta.ts`
- `docs/adr/ADR-006-factory-method-votify.md` (jerarquía EvaluacionComentario)
- `docs/adr/ADR-009-comentarios-por-criterio-en-voting.md` (distinción comentarios experto vs. votante)
