# Visión global: Factory Method EvaluacionCreator — antes de implementar

**Fecha:** 11-04-2026
**Sprint:** S1
**Referencia:** ADR-006, sección 4.1

---

## 1. Estructura actual del proyecto (ANTES — sin nuevas clases)

```
src/main/java/com/votify/
├── VotifyApplication.java
├── advice/
│   └── RestExceptionHandler.java
├── config/
│   ├── CorsConfig.java
│   ├── LocalDataSourceConfig.java
│   ├── PostgresUrlDataSourceConfiguration.java
│   └── WebConfig.java
├── controller/
│   ├── CategoryController.java
│   ├── CompetitorController.java
│   ├── CriterionController.java
│   ├── EventController.java
│   ├── ProjectController.java
│   ├── TimeWindowController.java
│   ├── UserController.java
│   ├── VoterController.java
│   └── VotingController.java
├── dto/
│   ├── CategoryCriterionPointsDto.java
│   ├── CategoryDto.java
│   ├── CommentDto.java
│   ├── CompetitorCommentDto.java
│   ├── CompetitorDto.java
│   ├── CreateCategoryRequest.java
│   ├── CriterionDto.java
│   ├── EventDto.java
│   ├── EventParticipationDto.java
│   ├── ProjectDto.java
│   ├── RegisterCompetitorRequest.java
│   ├── RegisterNewParticipantRequest.java
│   ├── RegisterParticipationRequest.java
│   ├── TimeWindowDto.java
│   ├── UserDto.java
│   ├── VoterDto.java
│   └── VotingDto.java
├── entity/
│   ├── Category.java
│   ├── CategoryCriterionPoints.java
│   ├── Comment.java
│   ├── Competitor.java
│   ├── Criterion.java
│   ├── Event.java
│   ├── EventParticipation.java
│   ├── ParticipationRole.java
│   ├── Project.java
│   ├── TimeWindow.java
│   ├── TipoEvaluacion.java          ← YA CREADO (enum)
│   ├── User.java
│   ├── Voter.java
│   ├── Voting.java
│   └── VotingType.java
├── persistence/
│   ├── CategoryCriterionPointsRepository.java
│   ├── CategoryRepository.java
│   ├── CommentRepository.java
│   ├── CompetitorRepository.java
│   ├── CriterionRepository.java
│   ├── EventParticipationRepository.java
│   ├── EventRepository.java
│   ├── ProjectRepository.java
│   ├── TimeWindowRepository.java
│   ├── UserRepository.java
│   ├── VoterRepository.java
│   └── VotingRepository.java
└── service/
    ├── CategoryService.java          ← ÚNICA CLASE QUE SE MODIFICARÁ
    ├── CompetitorService.java
    ├── CriterionService.java
    ├── EventParticipationService.java
    ├── EventService.java
    ├── ProjectService.java
    ├── TimeWindowService.java
    ├── UserService.java
    ├── VoterService.java
    ├── VotingService.java
    └── factory/
        └── participant/
            ├── CompetitorCreator.java
            ├── ParticipantCreator.java
            └── VoterCreator.java
```

---

## 2. Diagrama UML — Factory Method GoF (EvaluacionCreator)

**FigJam interactivo:** https://www.figma.com/online-whiteboard/create-diagram/194a3987-8657-4659-94e2-a45dee428cef

### Participantes del patrón GoF

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT                                      │
│                                                                     │
│  EvaluacionService                                                  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ Map<TipoEvaluacion, EvaluacionCreator> creators               │  │
│  │                                                               │  │
│  │ create(dto):                                                  │  │
│  │   tipo = TipoEvaluacion.valueOf(dto.getTipo())                │  │
│  │   creator = creators.get(tipo)     ← lookup, NO if/else      │  │
│  │   evaluacion = creator.createAndValidate(dto)                 │  │
│  │   // resolver relaciones (evaluador, competitor, category)    │  │
│  │   return repository.save(evaluacion)                          │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
          │ usa
          ▼
┌─────────────────────────────────────────┐
│      CREATOR (abstracto)                │
│                                         │
│  EvaluacionCreator                      │
│  ─────────────────────                  │
│  + create(dto): Evaluacion   {abstract} │ ← Factory Method
│  + getTipo(): TipoEvaluacion {abstract} │
│  + createAndValidate(dto): Evaluacion   │ ← Template Method (valida peso >= 0)
└─────────────────┬───────────────────────┘
                  │ extends
    ┌─────────────┼─────────────┬──────────────┬──────────────┬──────────────┐
    ▼             ▼             ▼              ▼              ▼              ▼
┌─────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌─────────┐ ┌─────────┐
│Numerica │ │Checklist │ │ Rubrica  │ │Comentario │ │  Audio  │ │  Video  │
│Creator  │ │ Creator  │ │ Creator  │ │  Creator  │ │ Creator │ │ Creator │
└────┬────┘ └────┬─────┘ └────┬─────┘ └─────┬─────┘ └────┬────┘ └────┬────┘
     │ creates    │ creates    │ creates     │ creates     │ creates   │ creates
     ▼            ▼            ▼             ▼             ▼           ▼
┌─────────────────────────────────────────┐
│      PRODUCT (abstracto)                │
│                                         │
│  Evaluacion  (@Entity, SINGLE_TABLE)    │
│  ─────────────────────                  │
│  - id: Long                             │
│  - evaluador: User                      │
│  - competitor: Competitor               │
│  - category: Category                   │
│  - criterion: Criterion (nullable)      │
│  - peso: Double                         │
│  - datos: String (JSON)                 │
│  - createdAt: Date                      │
│  ─────────────────────                  │
│  + calcularScore(): Double   {abstract} │
└─────────────────┬───────────────────────┘
                  │ extends
    ┌─────────────┼─────────────┬──────────────┬──────────────┬──────────────┐
    ▼             ▼             ▼              ▼              ▼              ▼
┌─────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌─────────┐ ┌─────────┐
│Evaluac. │ │Evaluac.  │ │Evaluac.  │ │ Evaluac.  │ │Evaluac. │ │Evaluac. │
│Numerica │ │Checklist │ │ Rubrica  │ │Comentario │ │  Audio  │ │  Video  │
│         │ │          │ │          │ │           │ │         │ │         │
│score=   │ │score=    │ │score=    │ │score=     │ │score=   │ │score=   │
│sum(vals)│ │%checked  │ │avg(n/m)  │ │null       │ │manual?  │ │manual?  │
└─────────┘ └──────────┘ └──────────┘ └───────────┘ └─────────┘ └─────────┘
```

---

## 3. Ficheros nuevos a crear (22 ficheros)

### Entidades (entity/)
| Fichero | Rol GoF | Descripción |
|---------|---------|-------------|
| `Evaluacion.java` | Product (abstracto) | Clase base @Entity SINGLE_TABLE con calcularScore() abstracto |
| `EvaluacionNumerica.java` | ConcreteProduct | score = suma de valores numéricos |
| `EvaluacionChecklist.java` | ConcreteProduct | score = % de items marcados × 100 |
| `EvaluacionRubrica.java` | ConcreteProduct | score = media ponderada de niveles × 100 |
| `EvaluacionComentario.java` | ConcreteProduct | score = null (cualitativa) |
| `EvaluacionAudio.java` | ConcreteProduct | score = scoreManual o null |
| `EvaluacionVideo.java` | ConcreteProduct | score = scoreManual o null |

### Factory (service/factory/evaluacion/)
| Fichero | Rol GoF | Descripción |
|---------|---------|-------------|
| `EvaluacionCreator.java` | Creator (abstracto) | create() + createAndValidate() + getTipo() |
| `EvaluacionNumericaCreator.java` | ConcreteCreator | Instancia EvaluacionNumerica |
| `EvaluacionChecklistCreator.java` | ConcreteCreator | Instancia EvaluacionChecklist |
| `EvaluacionRubricaCreator.java` | ConcreteCreator | Instancia EvaluacionRubrica |
| `EvaluacionComentarioCreator.java` | ConcreteCreator | Instancia EvaluacionComentario |
| `EvaluacionAudioCreator.java` | ConcreteCreator | Instancia EvaluacionAudio |
| `EvaluacionVideoCreator.java` | ConcreteCreator | Instancia EvaluacionVideo |

### DTO, Repository, Service, Controller
| Fichero | Descripción |
|---------|-------------|
| `dto/EvaluacionDto.java` | DTO con campos id, tipo, evaluadorId, competitorId, categoryId, criterionId, peso, datos, score, createdAt |
| `persistence/EvaluacionRepository.java` | JpaRepository con queries por category, competitor |
| `service/EvaluacionService.java` | Client del patrón — usa Map<TipoEvaluacion, EvaluacionCreator> |
| `controller/EvaluacionController.java` | REST endpoints /api/evaluaciones |

### Tests
| Fichero | Tipo |
|---------|------|
| `test/.../service/EvaluacionServiceTest.java` | Unitario (Mockito) |
| `test/.../service/factory/evaluacion/EvaluacionCreatorTest.java` | Unitario (Mockito) |
| `test/.../persistence/EvaluacionRepositoryTest.java` | Integración (H2) |

---

## 4. Clase existente que se modifica: CategoryService.java

### Ubicación
`src/main/java/com/votify/service/CategoryService.java`

### Cambio previsto
En el método `delete(Long id)` (líneas 115-125), añadir la llamada a `evaluacionRepository.deleteByCategoryId(id)` para cascade delete de evaluaciones al borrar una categoría.

### ANTES (estado actual — líneas 115-125)

```java
@Transactional
public void delete(Long id) {
    if (!categoryRepository.existsById(id)) {
        throw new RuntimeException("Category not found with id: " + id);
    }
    // Delete votings, event participations, criterion points linked to this category
    votingRepository.deleteByCategoryId(id);
    eventParticipationRepository.deleteByCategoryId(id);
    criterionPointsRepository.deleteByCategoryId(id);
    categoryRepository.deleteById(id);
}
```

### DESPUÉS (cambio previsto)

```java
@Transactional
public void delete(Long id) {
    if (!categoryRepository.existsById(id)) {
        throw new RuntimeException("Category not found with id: " + id);
    }
    // Delete evaluaciones, votings, event participations, criterion points linked to this category
    evaluacionRepository.deleteByCategoryId(id);    // ← LÍNEA NUEVA
    votingRepository.deleteByCategoryId(id);
    eventParticipationRepository.deleteByCategoryId(id);
    criterionPointsRepository.deleteByCategoryId(id);
    categoryRepository.deleteById(id);
}
```

**Cambios adicionales en CategoryService.java:**
- Añadir import: `import com.votify.persistence.EvaluacionRepository;`
- Añadir campo: `private final EvaluacionRepository evaluacionRepository;`
- Añadir parámetro al constructor

### Constructor ANTES (líneas 35-47)

```java
public CategoryService(CategoryRepository categoryRepository,
                       EventRepository eventRepository,
                       CriterionRepository criterionRepository,
                       CategoryCriterionPointsRepository criterionPointsRepository,
                       VotingRepository votingRepository,
                       EventParticipationRepository eventParticipationRepository) {
    this.categoryRepository = categoryRepository;
    this.eventRepository = eventRepository;
    this.criterionRepository = criterionRepository;
    this.criterionPointsRepository = criterionPointsRepository;
    this.votingRepository = votingRepository;
    this.eventParticipationRepository = eventParticipationRepository;
}
```

### Constructor DESPUÉS

```java
public CategoryService(CategoryRepository categoryRepository,
                       EventRepository eventRepository,
                       CriterionRepository criterionRepository,
                       CategoryCriterionPointsRepository criterionPointsRepository,
                       VotingRepository votingRepository,
                       EventParticipationRepository eventParticipationRepository,
                       EvaluacionRepository evaluacionRepository) {
    this.categoryRepository = categoryRepository;
    this.eventRepository = eventRepository;
    this.criterionRepository = criterionRepository;
    this.criterionPointsRepository = criterionPointsRepository;
    this.votingRepository = votingRepository;
    this.eventParticipationRepository = eventParticipationRepository;
    this.evaluacionRepository = evaluacionRepository;
}
```

---

## 5. Ninguna otra clase existente se modifica

Todas las demás clases (entidades, DTOs, repos, services, controllers, tests) se mantienen intactas. Solo `CategoryService.java` recibe una modificación mínima (1 import, 1 campo, 1 parámetro de constructor, 1 línea en delete()).
