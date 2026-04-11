# Plan: Implementación de EvaluacionCreator (Factory Method GoF) + Documentación

## Contexto

El ADR-006 define el Factory Method para el dominio `Evaluacion` con 6 tipos (NUMERICA, CHECKLIST, RUBRICA, COMENTARIO, AUDIO, VIDEO), cada uno con lógica `calcularScore()` diferente. Actualmente la evaluación numérica funciona vía la entidad `Voting` (sliders por criterio), pero no existe un dominio `Evaluacion` separado. Se decide implementarlo ahora como parte del Sprint 1 (UT "Evaluar Proyecto"), junto con la actualización del ADR-006 y la creación del IA log correspondiente.

---

## Parte 1: Implementación del Factory Method (solo backend)

### Paso 1 — Enum `TipoEvaluacion`
- **Fichero nuevo:** `src/main/java/com/votify/entity/TipoEvaluacion.java`
- Valores: `NUMERICA, CHECKLIST, RUBRICA, COMENTARIO, AUDIO, VIDEO`
- Patrón: igual que `VotingType.java`

### Paso 2 — Entidad abstracta `Evaluacion`
- **Fichero nuevo:** `src/main/java/com/votify/entity/Evaluacion.java`
- JPA: `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` + `@DiscriminatorColumn(name = "tipo")`
- **Razón SINGLE_TABLE vs JOINED:** los 6 subtipos difieren en comportamiento (`calcularScore()`), no en columnas. Evita 6 tablas JOIN extra. El ADR-006 mencionaba este riesgo — se resuelve aquí.
- Campos:
  - `id` (Long, @Id @GeneratedValue)
  - discriminator `tipo` (automático vía @DiscriminatorColumn)
  - `evaluador` (User, @ManyToOne)
  - `competitor` (Competitor, @ManyToOne)
  - `category` (Category, @ManyToOne)
  - `criterion` (Criterion, @ManyToOne, nullable)
  - `peso` (Double) — peso/weight del criterio
  - `datos` (String, TEXT) — JSON con datos específicos del tipo
  - `createdAt` (Date, @Temporal)
- Método abstracto: `public abstract Double calcularScore();`

### Paso 3 — 6 Entidades concretas (ConcreteProduct)
- Todos en `src/main/java/com/votify/entity/`
- Cada uno: `@Entity`, `@DiscriminatorValue("TIPO")`, override de `calcularScore()`

| Clase | calcularScore() |
|-------|----------------|
| `EvaluacionNumerica` | Parsea JSON `{"valores": [8,7,9]}`, retorna suma |
| `EvaluacionChecklist` | Parsea `{"items": [true,false,true]}`, retorna % de true × 100 |
| `EvaluacionRubrica` | Parsea `{"niveles": [{"nivel":3,"max":5}]}`, retorna media ponderada × 100 |
| `EvaluacionComentario` | Retorna `null` (cualitativa, sin score) |
| `EvaluacionAudio` | Parsea `{"url":"...","scoreManual":85}`, retorna scoreManual o null |
| `EvaluacionVideo` | Igual que Audio |

### Paso 4 — DTO
- **Fichero nuevo:** `src/main/java/com/votify/dto/EvaluacionDto.java`
- Campos: id, tipo (String), evaluadorId, competitorId, categoryId, criterionId, peso, datos (JSON string), score (calculado), createdAt
- Patrón: igual que `VotingDto.java`

### Paso 5 — Repository
- **Fichero nuevo:** `src/main/java/com/votify/persistence/EvaluacionRepository.java`
- `JpaRepository<Evaluacion, Long>`
- Queries: findByCategoryId, findByCompetitorId, findByCategoryIdAndCompetitorId, deleteByCategoryId

### Paso 6 — Factory Method (Creator + 6 ConcreteCreators)
- **Directorio:** `src/main/java/com/votify/service/factory/evaluacion/`
- **Patrón a seguir:** `ParticipantCreator.java` existente

**`EvaluacionCreator.java`** (abstracto):
- `abstract Evaluacion create(EvaluacionDto dto)` — factory method
- `Evaluacion createAndValidate(EvaluacionDto dto)` — template method: valida peso >= 0, delega en create()
- `abstract TipoEvaluacion getTipo()`

**6 ConcreteCreators:** `EvaluacionNumericaCreator`, `EvaluacionChecklistCreator`, `EvaluacionRubricaCreator`, `EvaluacionComentarioCreator`, `EvaluacionAudioCreator`, `EvaluacionVideoCreator`
- Cada uno: instancia el ConcreteProduct correcto, setea peso y datos

### Paso 7 — Service
- **Fichero nuevo:** `src/main/java/com/votify/service/EvaluacionService.java`
- `Map<TipoEvaluacion, EvaluacionCreator>` para seleccionar creator (el único "if/else" restante se convierte en un map lookup)
- CRUD completo: create, findAll, findById, update, delete
- `create()`: parsea tipo → busca creator → createAndValidate → resuelve relaciones via repos → save → toDto con calcularScore()
- Patrón: igual que `VotingService.java`

### Paso 8 — Controller
- **Fichero nuevo:** `src/main/java/com/votify/controller/EvaluacionController.java`
- `@RequestMapping("/api/evaluaciones")`
- Endpoints: GET all, GET by id, POST, PUT, DELETE, GET by-category, GET by-competitor, GET by-category-competitor
- Patrón: igual que `VotingController.java`

### Paso 9 — Tests
**Unitarios (Mockito):**
- `src/test/java/com/votify/service/EvaluacionServiceTest.java` — CRUD + validación peso + cada tipo produce subclase correcta
- `src/test/java/com/votify/service/factory/evaluacion/EvaluacionCreatorTest.java` — cada ConcreteCreator produce el tipo correcto, createAndValidate rechaza peso < 0

**Integración (H2):**
- `src/test/java/com/votify/persistence/EvaluacionRepositoryTest.java` — persistencia SINGLE_TABLE, queries custom, cascade delete

### Paso 10 — Cascade en CategoryService
- Añadir llamada a `evaluacionRepository.deleteByCategoryId()` en el flujo de borrado de categoría, igual que existe `votingRepository.deleteByCategoryId()`

---

## Parte 2: Actualización ADR-006

- **Fichero:** `docs/adr/ADR-006-factory-method-votify.md`
- Cambiar estado de "Propuesto — pendiente de implementación" a "Implementado parcialmente (Sprint 1)"
- En sección 4.1 (`EvaluacionCreator`): marcar como implementado, documentar decisión SINGLE_TABLE
- Añadir sección sobre la decisión de `datos` como columna JSON TEXT
- Actualizar la estructura de ficheros (sección 7) para reflejar la realidad: entidades en `entity/` (no en `domain/evaluacion/`)

---

## Parte 3: IA Log — Sesión Factory Method EvaluacionCreator

- Añadir como **Sesión 3** al final de `docs/ai-logs/Sprint-S1-ai-log.md`
- Formato: seguir exactamente el de las Sesiones 1 y 2 existentes
- Secciones requeridas:
  - **6.2 Revisión crítica de la solución propuesta por la IA:** análisis de pros/contras de SINGLE_TABLE, JSON datos, trade-offs del patrón
  - **6.3 Adaptación de la solución a la aplicación:** cómo se adaptó el patrón GoF genérico al contexto específico de Votify (entidades JPA, Spring repos, Map de creators vs herencia pura)

---

## Ficheros totales a crear/modificar

**Nuevos (~22 ficheros):**
- 1 enum + 7 entidades + 1 DTO + 1 repo + 7 factory + 1 service + 1 controller + 3 test files

**Modificados (3 ficheros):**
- `CategoryService.java` — cascade delete de evaluaciones
- `docs/adr/ADR-006-factory-method-votify.md` — actualización estado
- `docs/ai-logs/Sprint-S1-ai-log.md` — nueva sesión 3

---

## Verificación

1. `mvn test` — los 98 tests existentes siguen pasando
2. Tests nuevos de EvaluacionCreator y EvaluacionService pasan
3. Tests de integración verifican persistencia SINGLE_TABLE en H2
4. `mvn clean spring-boot:run` — arranque sin errores
5. Endpoint `POST /api/evaluaciones` con `tipo: "NUMERICA"` y `datos: {"valores":[8,7,9]}` retorna score calculado
