# Resumen del Backend — Votify

**Stack:** Spring Boot 3.2.0 · Java 21 · Spring Data JPA · PostgreSQL (Supabase) · Maven
**Tests:** 98 tests (73 unitarios Mockito + 25 integración @DataJpaTest con H2)
**Arquitectura:** Frontend React → Axios `/api` → Spring Boot :8080 → PostgreSQL Supabase
**Última actualización:** Sprint 1 — 07-04-2026

---

## Estructura de paquetes

```
com.votify
├── entity/          → Entidades JPA (tablas de la BD)
├── dto/             → Objetos de transferencia de datos (entrada/salida de la API)
├── persistence/     → Repositorios Spring Data (acceso a BD)
├── service/         → Lógica de negocio
├── controller/      → Endpoints REST (HTTP)
├── config/          → CORS, DataSource
└── advice/          → Manejo global de excepciones
```

---

## 1. Capa de Entidades (`entity/`)

Son las clases que Hibernate mapea a tablas de PostgreSQL. Aquí vive el modelo de dominio.

### Jerarquía de personas (herencia JPA JOINED)

```
User  (tabla: users)
 └── Participant  (tabla: participants)
      ├── Competitor  (tabla: competitors)
      └── Voter       (tabla: voters)
```

| Clase | Tabla | Campos | Notas |
|---|---|---|---|
| `User` | `users` | `id`, `name` (NOT NULL), `email` (UNIQUE NOT NULL) | Raíz de herencia con `@Inheritance(JOINED)`. Método `createEvent(name, timeInitial, timeFinal)`. |
| `Participant` | `participants` | hereda `User` | `@PrimaryKeyJoinColumn(name="user_id")`. Sin campos propios. |
| `Competitor` | `competitors` | hereda `Participant` | Método `createProjectForEvent(name, description, event) → Project`. |
| `Voter` | `voters` | hereda `Participant` | Sin campos extra. Referenciado desde `Voting`. |

---

### Entidades de dominio principales

| Clase | Tabla | Campos clave | Relaciones |
|---|---|---|---|
| `Event` | `events` | `id`, `name` (NOT NULL), `timeInitial`, `timeFinal` (TIMESTAMP), `reminderMinutes`, `reminderHours`, `organizer_id` (FK opcional) | → `User` (organizer, ManyToOne LAZY) · → `Category` (OneToMany cascade ALL) · → `Project` (OneToMany cascade ALL) |
| `Category` | `categories` | `id`, `name` (NOT NULL), `voting_type` (enum), `timeInitial`, `timeFinal`, `reminder_minutes`, **`total_points`** (req.23, POPULAR_VOTE), **`max_votes_per_voter`** (req.19, POPULAR_VOTE) | → `Event` (ManyToOne LAZY) · → `CategoryCriterionPoints` (OneToMany cascade ALL) |
| `VotingType` | *(enum)* | `JURY_EXPERT`, `POPULAR_VOTE` | — |
| `Project` | `projects` | `id`, `name` (NOT NULL), `description` (length=1000), `event_id` (FK) | → `Event` (ManyToOne LAZY) · → `Competitor` (ManyToMany via `project_competitors`) |
| `Criterion` | `criteria` | `id`, `name` (NOT NULL) | — |
| `CategoryCriterionPoints` | `category_criterion_points` | `id`, `category_id`, `criterion_id`, `max_points` (NOT NULL) | UNIQUE(category_id, criterion_id). Suma de todos los `max_points` de una categoría = 100 (JURY_EXPERT). → `Category` · → `Criterion` (ManyToOne LAZY) |
| `Voting` | `votings` | `id`, `voter_id` (NOT NULL), `competitor_id` (NOT NULL), `criterion_id` (NOT NULL), `category_id` (opcional para POPULAR_VOTE), `score` (NOT NULL), `manually_modified` (Boolean) | → `Voter` · → `Competitor` · → `Criterion` · → `Category` (todos ManyToOne) |
| `EventParticipation` | `event_participations` | `id`, `event_id`, `user_id`, `category_id`, `role` (enum) | UNIQUE(event_id, user_id, category_id). Implementa **composición de roles** (ADR-002). → `Event` · → `User` · → `Category` |
| `ParticipationRole` | *(enum)* | `COMPETITOR`, `VOTER` | — |
| `Comment` | `comments` | `id`, `text` (length=2000, NOT NULL), `voter_id` (NOT NULL), `project_id` (NOT NULL) | → `Project` · → `Voter` |
| `TimeWindow` | `time_windows` | `id`, `startTime`, `endTime` (LocalDateTime) | Entidad auxiliar para periodos de votación. |

---

## 2. Capa de DTOs (`dto/`)

Los DTOs son los objetos que viajan por la API. **Nunca se exponen entidades directamente al exterior.**

| DTO | Campos | Sprint |
|---|---|---|
| `UserDto` | `id`, `name`, `email` | S0 |
| `ParticipantDto` extends UserDto | (heredado) | S0 |
| `CompetitorDto` extends ParticipantDto | (heredado) | S0 |
| `VoterDto` extends ParticipantDto | (heredado) | S0 |
| `EventDto` | `id`, `name`, `timeInitial`, `timeFinal`, `organizerId`, `creator`, `categories`, `participants`, `projects`, `reminderMinutes`, `reminderHours` | S0+S1 |
| `CategoryDto` | `id`, `name`, `votingType`, `timeInitial`, `timeFinal`, `eventId`, `reminderMinutes`, **`totalPoints`**, **`maxVotesPerVoter`** | S0+S1 |
| `CriterionDto` | `id`, `name` | S0 |
| `ProjectDto` | `id`, `name`, `description`, `eventId`, `competitorIds` | S0+S1 |
| `CommentDto` | `id`, `voterId`, `text`, `projectId` | S0 |
| `CompetitorCommentDto` | `id`, `text`, `voterId`, `projectId`, `projectName` | S1 |
| `VotingDto` | `id`, `voterId`, `competitorId`, `criterionId`, `categoryId`, `score`, `manuallyModified` | S0+S1 |
| `EventParticipationDto` | `id`, `eventId`, `userId`, `categoryId`, `role`, `userName`, `userEmail` (alias `email`), `categoryName` | S0+S1 |
| `CategoryCriterionPointsDto` | `id`, `categoryId`, `criterionId`, `criterionName`, `maxPoints` | S0 |
| `TimeWindowDto` | `id`, `startTime`, `endTime` | S0 |
| `CreateCategoryRequest` | `name` | S0 |
| `RegisterCompetitorRequest` | `userId`, `categoryId` | S0 |
| `RegisterNewParticipantRequest` | `name`, `email`, `categoryId` — crea User+Participant+EventParticipation | S1 |
| `RegisterParticipationRequest` | `userId`, `categoryId`, `role` (ParticipationRole) | S0 |

---

## 3. Capa de Repositorios (`persistence/`)

Interfaces que extienden `JpaRepository<Entidad, Long>`. Los métodos custom usan query derivation o `@Query` manual.

| Repositorio | Entidad | Métodos custom destacados |
|---|---|---|
| `UserRepository` | `User` | `findByEmail(String)` |
| `ParticipantRepository` | `Participant` | — |
| `CompetitorRepository` | `Competitor` | — |
| `VoterRepository` | `Voter` | — |
| `EventRepository` | `Event` | — |
| `CategoryRepository` | `Category` | `findByEventId(Long)` |
| `CriterionRepository` | `Criterion` | — |
| `ProjectRepository` | `Project` | `findByEventId(Long)` |
| `CommentRepository` | `Comment` | `findByProjectId(Long)`, `findByProjectIdIn(List<Long>)`, `deleteByProjectIdIn(List<Long>)` |
| `CategoryCriterionPointsRepository` | `CategoryCriterionPoints` | `findByCategoryId(Long)`, `findByCategoryIdAndCriterionId(Long, Long)`, `deleteByCategoryId(Long)`, `deleteByCategoryIdIn(List<Long>)` |
| `EventParticipationRepository` | `EventParticipation` | `findByEventId`, `findByEventIdAndCategoryId`, `findByEventIdAndCategoryIdAndRole`, `existsByEventIdAndUserIdAndCategoryId`, `findByEventIdAndUserIdAndCategoryId`, `findByUserId`, `deleteByEventId`, `deleteByCategoryId` |
| `VotingRepository` | `Voting` | `findByCompetitorIdIn(List<Long>)`, `findByVoterIdAndCompetitorId(Long, Long)`, `findByVoterIdAndCategoryId(Long, Long)`, `deleteByCategoryIdIn(List<Long>)` @Modifying, `deleteByCategoryId(Long)` @Modifying, `findDistinctVoterIdsByCategoryId(Long)` @Query, **`countDistinctCompetitorsByVoterIdAndCategoryId(voterId, categoryId)`** @Query, **`sumScoreByVoterIdAndCategoryId(voterId, categoryId)`** @Query, **`findExistingVote(voterId, competitorId, criterionId, categoryId)`** @Query |
| `TimeWindowRepository` | `TimeWindow` | — |

---

## 4. Capa de Servicios (`service/`)

Toda la lógica de negocio. Usan **inyección por constructor** (sin `@Autowired` en campos). Lanzan `RuntimeException` para errores de validación (capturado en `RestExceptionHandler`).

---

### `EventService`
**Llama a:** `EventRepository`, `UserRepository`, `EventParticipationService`

| Método | Descripción |
|---|---|
| `findAll()` / `findById(id)` | Consultas estándar |
| `create(CreateEventRequest)` | Crea evento con categorías y registra al creador como COMPETITOR en su categoría |
| `createForOrganizer(organizerId, EventDto)` | Usa `User.createEvent()` para asociar el organizador |
| `update(id, EventDto)` | Actualiza nombre, fechas, reminderMinutes/Hours |
| `delete(id)` @Transactional | Cascade: elimina votings, participaciones, comentarios, criterion points |
| `resolveReminderMinutes(dto)` | Helper: convierte `reminderHours` a minutos si no hay `reminderMinutes` |

---

### `CategoryService`
**Llama a:** `CategoryRepository`, `EventRepository`, `CriterionRepository`, `CategoryCriterionPointsRepository`

| Método | Descripción |
|---|---|
| CRUD estándar | `findAll`, `findById`, `create`, `update`, `delete` @Transactional |
| `findByEventId(eventId)` | Categorías de un evento |
| `createForEvent(eventId, name)` | Crea categoría enlazada al evento |
| `setVotingType(id, VotingType)` | Asigna JURY_EXPERT o POPULAR_VOTE |
| `getCriterionPoints(categoryId)` | Lista puntos por criterio de una categoría |
| `setCriterionPoints(categoryId, criterionId, maxPoints)` | Upsert individual; valida total acumulado ≤ 100 |
| `setCriterionPointsBulk(categoryId, dtos)` @Transactional | Reemplaza toda la config; exige suma = 100 exacta |
| `deleteCriterionPoints(categoryId, criterionId)` | Elimina config de un criterio concreto |
| **`setTotalPoints(categoryId, totalPoints)`** | (req.23) Config de puntos totales para POPULAR_VOTE |
| **`getTotalPoints(categoryId)`** | Devuelve totalPoints de la categoría |
| **`setMaxVotesPerVoter(categoryId, maxVotesPerVoter)`** | (req.19) Límite de competidores distintos por votante |
| `validateCategoryTimesWithinEvent(event, start, end)` | Valida que las fechas de la categoría estén dentro del rango del evento |

---

### `CriterionService`
CRUD simple: `findAll`, `findById`, `create`, `update`, `delete`. Sin lógica adicional.

---

### `ProjectService`
**Llama a:** `ProjectRepository`, `EventRepository`, `CompetitorRepository`, `VoterRepository`, `CommentRepository`

| Método | Descripción |
|---|---|
| `findAll()` / `findByEvent(eventId)` | Consultas |
| `createForEvent(eventId, dto)` | Crea proyecto enlazado al evento |
| `createForParticipantInEvent(participantId, eventId, dto)` | Crea proyecto asignando al participante como competidor |
| `addCompetitor(projectId, competitorId)` | Añade competidor al proyecto |
| `addComment(projectId, dto)` | Guarda un `Comment` asociado al proyecto y al voter |
| `getCommentsByProject(projectId)` | Lista comentarios de un proyecto |
| `getCompetitorIds(projectId)` | Lista IDs de competidores en el proyecto |

---

### `VotingService`
**Llama a:** `VotingRepository`, `VoterRepository`, `CompetitorRepository`, `CriterionRepository`, `CategoryRepository`

| Método | Descripción |
|---|---|
| CRUD estándar | `findAll`, `findById`, `delete` |
| `create(VotingDto)` @Transactional | Resuelve voter/competitor/criterion, aplica `evaluateVote()` y persiste |
| `update(id, VotingDto)` | Modifica score existente (intervención manual). Score=0 es válido (anular voto). Marca `manuallyModified=true`. |
| `findByCompetitorIds(ids)` | Votos de una lista de competidores |
| `findByVoterAndCompetitor(voterId, competitorId)` | Votos de un voter+competitor concreto |
| `getActiveVoterIds(categoryId)` | IDs de voters que han votado en la categoría |
| **`evaluateVote(voting)`** | Aplica las reglas de negocio según `VotingType` |
| **`validatePopularVoteRestrictions(voterId, competitorId, category, score)`** | (req.19) Valida que el votante no supera `maxVotesPerVoter` distintos. (req.23) Valida que el score total del voter no supera `totalPoints`. |
| **`findExistingVote(...)`** | Si ya existe un voto (voter+competitor+criterion+category), incrementa el score en lugar de crear uno nuevo |

**Reglas aplicadas en `evaluateVote()`:**
- JURY_EXPERT: `score ≤ maxPoints` del criterio en la categoría
- POPULAR_VOTE: valida restricciones req.19 (max competidores) y req.23 (max puntos totales)

---

### `EventParticipationService`
**Llama a:** `EventParticipationRepository`, `EventRepository`, `UserRepository`, `CategoryRepository`

| Método | Descripción |
|---|---|
| `registerParticipation(eventId, userId, categoryId, role)` | Valida que categoría pertenece al evento, constraint UNIQUE(event, user, category), y persiste |
| `registerCompetitor(...)` / `registerVoter(...)` | Wrappers con rol fijado |
| `registerNewCompetitor(eventId, name, email, categoryId)` @Transactional | Valida email regex + nombre no vacío → crea `Competitor` + `EventParticipation` |
| `registerNewVoter(eventId, name, email, categoryId)` @Transactional | Idem para `Voter` |
| `getParticipationsByEvent(eventId)` | Lista participaciones de un evento |
| `getParticipationsByEventAndCategory(eventId, categoryId)` | Lista por evento+categoría |
| `getCompetitorsByEventAndCategory(...)` / `getVotersByEventAndCategory(...)` | Filtra por rol |
| `getParticipationsByUser(userId)` | Participaciones de un usuario |
| `removeParticipation(eventId, userId, categoryId)` | Da de baja a un participante |

---

### `UserService`, `ParticipantService`, `CompetitorService`, `VoterService`, `TimeWindowService`
CRUD estándar (`findAll`, `findById`, `create`, `update`, `delete`). Sin lógica de negocio adicional.

---

## 5. Capa de Controllers (`controller/`)

| Controller | Ruta base | Endpoints |
|---|---|---|
| `UserController` | `/api/users` | GET `/`, GET `/{id}`, POST `/`, PUT `/{id}`, DELETE `/{id}` |
| `EventController` | `/api/events` | GET `/`, GET `/{id}`, POST `/`, POST `/by-organizer`, PUT `/{id}`, DELETE `/{id}` (cascade), GET `/{id}/categories`, POST `/{id}/categories`, GET `/{id}/participations`, POST `/{id}/participations`, GET `/{id}/categories/{catId}/participations`, GET `/{id}/categories/{catId}/competitors`, GET `/{id}/categories/{catId}/voters`, POST `/{id}/competitors`, POST `/{id}/voters`, **POST `/{id}/competitors/register`**, **POST `/{id}/voters/register`**, **DELETE `/{id}/participations?userId=&categoryId=`** |
| `CategoryController` | `/api/categories` | GET `/`, GET `/{id}`, POST `/`, PUT `/{id}`, DELETE `/{id}` (cascade), PUT `/{id}/voting-type?type=`, GET `/{id}/criterion-points`, PUT `/{id}/criterion-points/{criterionId}`, PUT `/{id}/criterion-points/bulk`, DELETE `/{id}/criterion-points/{criterionId}`, **GET `/{id}/total-points`**, **PUT `/{id}/total-points`**, **PUT `/{id}/max-votes-per-voter`**, GET `/{id}/active-voters` |
| `CriterionController` | `/api/criteria` | CRUD estándar |
| `ProjectController` | `/api` | GET `/projects`, POST `/events/{id}/projects`, GET `/events/{id}/projects`, POST `/projects/{id}/competitors/{competitorId}`, POST `/projects/{id}/comments`, GET `/projects/{id}/comments`, GET `/projects/{id}/competitors` |
| `VotingController` | `/api/votings` | GET `/`, GET `/{id}`, POST `/`, PUT `/{id}`, DELETE `/{id}`, GET `/by-competitors?ids=`, GET `/by-voter-competitor?voterId=&competitorId=` |
| `CompetitorController` | `/api/competitors` | CRUD estándar + GET `/{competitorId}/comments` |
| `VoterController` | `/api/voters` | CRUD estándar |
| `ParticipantController` | `/api/participants` | CRUD estándar |
| `TimeWindowController` | `/api/time-windows` | CRUD estándar |

> **Negrita** = endpoints añadidos en Sprint 1

---

## 6. Diagrama de dependencias entre servicios

```
EventService
  ├── EventRepository
  ├── UserRepository
  └── EventParticipationService
        ├── EventParticipationRepository
        ├── EventRepository
        ├── UserRepository
        └── CategoryRepository

CategoryService
  ├── CategoryRepository
  ├── EventRepository
  ├── CriterionRepository
  └── CategoryCriterionPointsRepository

VotingService
  ├── VotingRepository
  ├── VoterRepository
  ├── CompetitorRepository
  ├── CriterionRepository
  └── CategoryRepository          ← añadido S1 (para leer totalPoints/maxVotesPerVoter)

ProjectService
  ├── ProjectRepository
  ├── EventRepository
  ├── CompetitorRepository
  ├── VoterRepository
  └── CommentRepository
```

---

## 7. Tablas en BD generadas por Hibernate

| Tabla | Entidad JPA |
|---|---|
| `users` | `User` |
| `participants` | `Participant` (FK → users) |
| `competitors` | `Competitor` (FK → participants) |
| `voters` | `Voter` (FK → participants) |
| `events` | `Event` |
| `categories` | `Category` |
| `criteria` | `Criterion` |
| `projects` | `Project` |
| `project_competitors` | tabla join ManyToMany Project ↔ Competitor |
| `category_criterion_points` | `CategoryCriterionPoints` |
| `votings` | `Voting` |
| `event_participations` | `EventParticipation` |
| `time_windows` | `TimeWindow` |
| `comments` | `Comment` |

---

## 8. Reglas de negocio implementadas

| # | Regla | Dónde se valida |
|---|---|---|
| 1 | `EventParticipation` UNIQUE(event, user, category) | `EventParticipationService.registerParticipation()` + constraint BD |
| 2 | `CategoryCriterionPoints` UNIQUE(category, criterion) | constraint BD |
| 3 | Suma de `maxPoints` de los criterios de una categoría = 100 (bulk) | `CategoryService.setCriterionPointsBulk()` |
| 4 | Suma acumulada de `maxPoints` por criterio individual ≤ 100 | `CategoryService.setCriterionPoints()` |
| 5 | Score de un voto JURY_EXPERT ≤ `maxPoints` del criterio | `VotingService.evaluateVote()` |
| 6 | (req.19) Votante no puede votar a más de `maxVotesPerVoter` competidores distintos | `VotingService.validatePopularVoteRestrictions()` |
| 7 | (req.23) Score total de un votante en POPULAR_VOTE ≤ `totalPoints` de la categoría | `VotingService.validatePopularVoteRestrictions()` |
| 8 | Si ya existe voto (voter+competitor+criterion+category), se incrementa el score existente | `VotingService.findExistingVote()` |
| 9 | Fechas de categoría dentro del rango del evento | `CategoryService.validateCategoryTimesWithinEvent()` |
| 10 | Email válido + nombre no vacío al registrar un participante nuevo | `EventParticipationService.registerNewCompetitor/Voter()` |
| 11 | Cascade delete: borrar evento elimina categorías, proyectos, votings, participaciones, comentarios | `EventService.delete()` @Transactional |

---

## 9. Tests

| Clase | Tipo | Tests | Qué valida |
|---|---|---|---|
| `CategoryServiceTest` | Unitario (Mockito) | **23** | CRUD, tipos de voto, puntos criterio (bulk, individual, excede 100, negativos), fechas, totalPoints, maxVotesPerVoter |
| `VotingServiceTest` | Unitario (Mockito) | **15** | Votos, intervención manual, score=0, validación POPULAR_VOTE (req.19/23), vote dedup |
| `EventServiceTest` | Unitario (Mockito) | 9 | CRUD eventos, organizador, cascade delete |
| `ProjectServiceTest` | Unitario (Mockito) | 9 | Proyectos, comentarios, competidores |
| `EventParticipationServiceTest` | Unitario (Mockito) | 8 | Registro, duplicados, validación categoría, email/nombre |
| `CriterionServiceTest` | Unitario (Mockito) | 9 | CRUD criterios |
| `CategoryRepositoryTest` | Integración (@DataJpaTest H2) | 9 | Queries JPA, findByEventId |
| `VotingRepositoryTest` | Integración (@DataJpaTest H2) | 8 | Persistencia votos, queries custom @Query |
| `EventParticipationRepositoryTest` | Integración (@DataJpaTest H2) | 8 | Queries complejas participaciones |
| **Total** | | **98** | |

Configuración: tests unitarios usan **Mockito** (sin contexto Spring); tests de integración usan **@DataJpaTest + H2** en memoria con `MODE=PostgreSQL` (ver ADR-004).

---

## 10. Configuración y despliegue

### Configuración CORS (`WebConfig` / `CorsConfig`)
- **Rutas:** `/api/**`
- **Orígenes permitidos:** `http://localhost:*` (local), `https://*.vercel.app` (Vercel), `https://*.onrender.com` (Render)
- **Métodos:** GET, POST, PUT, PATCH, DELETE, OPTIONS
- **Headers:** `*` — Max age: 3600s

### application.properties (producción)
```properties
server.port=${PORT:8080}
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
spring.datasource.url=jdbc:postgresql://aws-1-eu-west-1.pooler.supabase.com:6543/postgres?prepareThreshold=0
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
management.endpoints.web.exposure.include=health,info
```

### Perfil local
- Credenciales hardcodeadas en `config/LocalDataSourceConfig.java` (`@Profile("local") @Primary`)
- Credenciales en `application-local.properties` (gitignored)

### Comandos
```bash
# Arrancar (Windows PowerShell — requiere Java 21 en PATH)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn clean spring-boot:run

# Solo tests
mvn test

# Build JAR
mvn clean package -DskipTests
```

---

## 11. Decisiones de arquitectura (ADRs)

| ADR | Título | Estado |
|---|---|---|
| ADR-001 | Spring Boot como API REST (no acceso directo Supabase) | Aprobado |
| ADR-002 | Herencia JPA JOINED + `EventParticipation` para composición de roles | Aprobado |
| ADR-003 | Validación puntos criterio: individual (≤100) + bulk (=100 exacto) | Aprobado |
| ADR-004 | H2 en memoria para tests @DataJpaTest (no TestContainers en Sprint 0) | Aprobado |
| ADR-005 | Migración frontend: de Supabase JS directo a Spring Boot REST | Completado |
| ADR-006 | Factory Method GoF en dominios Evaluacion, Votacion y Rol | Propuesto |
