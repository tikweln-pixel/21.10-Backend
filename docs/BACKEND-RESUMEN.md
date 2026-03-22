# Resumen del Backend — Votify

**Stack:** Spring Boot 3.2.0 · Java 21 · Spring Data JPA · PostgreSQL (Supabase) · Maven
**Tests:** 86 tests (61 unitarios Mockito + 25 integración @DataJpaTest con H2)

---

## Estructura de paquetes

```
com.votify
├── entity/          → Entidades JPA (tablas de la BD)
├── dto/             → Objetos de transferencia de datos (lo que entra y sale por la API)
├── persistence/     → Repositorios Spring Data (acceso a BD)
├── service/         → Lógica de negocio
└── controller/      → Endpoints REST (HTTP)
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

| Clase | Tabla | Qué es | De qué se preocupa |
|---|---|---|---|
| `User` | `users` | Persona registrada en el sistema | `id`, `name`, `email` (único). Tiene el método `createEvent()` que construye un `Event` asociándose como organizador. Raíz de la herencia con `@Inheritance(JOINED)`. |
| `Participant` | `participants` | Extensión de `User` que puede participar en eventos | Hereda `User` con `@PrimaryKeyJoinColumn(name="user_id")`. Sin campos propios en Sprint 0. |
| `Competitor` | `competitors` | Participante que compite con un proyecto | Hereda `Participant`. Tiene `createProjectForEvent()`: crea un `Project` y se añade a sí mismo a sus competidores. |
| `Voter` | `voters` | Participante que puede votar | Hereda `Participant`. Sin campos extra en Sprint 0. Referenciado desde `Voting`. |

---

### Entidades de dominio principales

| Clase | Tabla | Qué es | De qué se preocupa | Relaciones clave |
|---|---|---|---|---|
| `Event` | `events` | Evento/hackathon/competición | `name`, `timeInitial`, `timeFinal`, `organizer`. Raíz del sistema. | → `User` (organizer, ManyToOne LAZY) · → `Category` (OneToMany, cascade ALL) · → `Project` (OneToMany, cascade ALL) |
| `Category` | `categories` | Agrupación dentro de un evento (ej. "Jurado", "Voto Popular") | `name`, `votingType` (enum), `timeInitial`, `timeFinal`, `reminderMinutes`. Define el tipo de votación y el periodo temporal. | → `Event` (ManyToOne LAZY) · → `CategoryCriterionPoints` (OneToMany, cascade ALL) |
| `VotingType` | *(enum)* | Tipo de votación de una categoría | Valores: `JURY_EXPERT` (Votacion_Jurado_Exp) y `POPULAR_VOTE` (Voto_Popular). — |
| `Project` | `projects` | Proyecto presentado por un equipo en un evento | `name`, `description`. Un proyecto pertenece a un `Event` y tiene varios `Competitor`. | → `Event` (ManyToOne LAZY) · → `Competitor` (ManyToMany via tabla `project_competitors`) |
| `Criterion` | `criteria` | Criterio de evaluación (ej. "Innovación", "Calidad Técnica") | Solo `name`. Sencillo a propósito: los pesos se gestionan en `CategoryCriterionPoints`. | — |
| `CategoryCriterionPoints` | `category_criterion_points` | Clase asociación: puntos máximos por criterio para cada categoría | `maxPoints`. Constraint UNIQUE `(category_id, criterion_id)`. La suma de todos los `maxPoints` de una categoría debe ser 100. | → `Category` (ManyToOne LAZY) · → `Criterion` (ManyToOne LAZY) |
| `Voting` | `votings` | Un voto: un `Voter` puntúa a un `Competitor` en un `Criterion` | `score` (Integer). Es la acción central del sistema. | → `Voter` (ManyToOne) · → `Competitor` (ManyToOne) · → `Criterion` (ManyToOne) |
| `EventParticipation` | `event_participations` | Registro de qué rol tiene un `User` en una `Category` de un `Event` | `role` (enum `ParticipationRole`). Constraint UNIQUE `(event_id, user_id, category_id)`. Implementa la **composición de roles** (ver ADR-002). | → `Event` (ManyToOne) · → `User` (ManyToOne) · → `Category` (ManyToOne) |
| `ParticipationRole` | *(enum)* | Rol de participación | Valores: `COMPETITOR`, `VOTER`. — |
| `TimeWindow` | `time_windows` | Ventana temporal genérica | `name`, fechas. Entidad auxiliar para periodos de votación. | — |
| `Comment` | `comments` | Comentario de un `Voter` sobre un `Project` | `text`, relación con `Project` y `Voter`. | → `Project` · → `Voter` |

---

## 2. Capa de DTOs (`dto/`)

Los DTOs son los objetos que viajan por la API (request/response). Los servicios los construyen desde entidades y los controllers los reciben/devuelven. **Nunca se exponen entidades directamente al exterior.**

| DTO | Qué transporta |
|---|---|
| `EventDto` | `id`, `name`, `timeInitial`, `timeFinal`, `organizerId` |
| `CreateEventRequest` | `name`, `categoryNames` (lista), `creatorUserId`, `creatorCategoryName` |
| `CategoryDto` | `id`, `name`, `votingType`, `timeInitial`, `timeFinal`, `eventId`, `reminderMinutes` |
| `CategoryCriterionPointsDto` | `id`, `categoryId`, `criterionId`, `criterionName`, `maxPoints` |
| `CriterionDto` | `id`, `name` |
| `ProjectDto` | `id`, `name`, `description`, `eventId` |
| `CompetitorDto` | `id`, `name`, `email` |
| `VoterDto` | `id`, `name`, `email` |
| `VotingDto` | `id`, `voterId`, `competitorId`, `criterionId`, `score` |
| `CommentDto` | `id`, `text`, `voterId`, `projectId` |
| `ParticipantDto` | `id`, `name`, `email` |
| `UserDto` | `id`, `name`, `email` |
| `EventParticipationDto` | `id`, `eventId`, `userId`, `categoryId`, `role` |
| `RegisterParticipationRequest` | `eventId`, `userId`, `categoryId` |
| `RegisterCompetitorRequest` | (idem, específico para competidores) |
| `TimeWindowDto` | `id`, `name`, fechas |

---

## 3. Capa de Repositorios (`persistence/`)

Interfaces que extienden `JpaRepository<Entidad, Long>`. Spring Data genera las queries automáticamente a partir del nombre del método. Sin lógica manual.

| Repositorio | Entidad | Métodos custom destacados |
|---|---|---|
| `EventRepository` | `Event` | — (solo CRUD estándar) |
| `CategoryRepository` | `Category` | `findByEventId(Long eventId)` |
| `CriterionRepository` | `Criterion` | — |
| `ProjectRepository` | `Project` | `findByEventId(Long eventId)` |
| `CompetitorRepository` | `Competitor` | — |
| `VoterRepository` | `Voter` | — |
| `VotingRepository` | `Voting` | — |
| `ParticipantRepository` | `Participant` | — |
| `UserRepository` | `User` | — |
| `CommentRepository` | `Comment` | — |
| `TimeWindowRepository` | `TimeWindow` | — |
| `CategoryCriterionPointsRepository` | `CategoryCriterionPoints` | `findByCategoryId(Long)`, `findByCategoryIdAndCriterionId(Long, Long)`, `deleteByCategoryId(Long)` |
| `EventParticipationRepository` | `EventParticipation` | `findByEventId`, `findByEventIdAndCategoryId`, `findByEventIdAndCategoryIdAndRole`, `existsByEventIdAndUserIdAndCategoryId`, `findByEventIdAndUserIdAndCategoryId`, `findByUserId` |

---

## 4. Capa de Servicios (`service/`)

Aquí reside toda la lógica de negocio. Los servicios son los únicos que conocen tanto los repositorios (para acceder a la BD) como los DTOs (para la comunicación con los controllers). Usan **inyección por constructor** (sin `@Autowired` en campos).

### `EventService`
**Se preocupa de:** ciclo de vida de los eventos.
**Llama a:** `EventRepository`, `UserRepository`, `EventParticipationService`.
**Operaciones clave:**
- `create(CreateEventRequest)` → crea el evento con sus categorías y registra al creador como COMPETITOR en su categoría.
- `createForOrganizer(organizerId, EventDto)` → usa `User.createEvent()` para asociar el organizador.
- CRUD estándar + `setTimeInitial/setTimeFinal` para actualizar fechas independientemente.

---

### `CategoryService`
**Se preocupa de:** gestión de categorías y su configuración de puntos y fechas.
**Llama a:** `CategoryRepository`, `EventRepository`, `CriterionRepository`, `CategoryCriterionPointsRepository`.
**Operaciones clave:**
- `create(CategoryDto)` → valida que las fechas de la categoría estén dentro del evento (`validateCategoryTimesWithinEvent`).
- `setVotingType(categoryId, VotingType)` → asigna JURY_EXPERT o POPULAR_VOTE.
- `setCriterionPoints(categoryId, criterionId, maxPoints)` → upsert individual; valida que el total acumulado ≤ 100.
- `setCriterionPointsBulk(categoryId, pointsDtos)` → reemplaza toda la config; exige suma = 100 exacta. `@Transactional`.
- `deleteCriterionPoints(categoryId, criterionId)` → elimina la config de un criterio concreto.

---

### `CriterionService`
**Se preocupa de:** CRUD simple de criterios de evaluación.
**Llama a:** `CriterionRepository`.
**Operaciones:** `findAll`, `findById`, `create`, `update`, `delete`. Sin lógica adicional.

---

### `ProjectService`
**Se preocupa de:** proyectos presentados por los competidores y comentarios asociados.
**Llama a:** `ProjectRepository`, `EventRepository`, `CompetitorRepository`, `VoterRepository`, `CommentRepository`.
**Operaciones clave:**
- `createForEvent(eventId, name, description)` → crea el proyecto enlazado al evento.
- `addComment(projectId, voterId, text)` → guarda un `Comment` asociado al proyecto y al voter.
- `addCompetitor(projectId, competitorId)` → añade un `Competitor` a la colección del proyecto y persiste.

---

### `VotingService`
**Se preocupa de:** registro y actualización de votos (intervención manual incluida).
**Llama a:** `VotingRepository`, `VoterRepository`, `CompetitorRepository`, `CriterionRepository`.
**Operaciones clave:**
- `create(VotingDto)` → resuelve las 3 entidades (voter, competitor, criterion) y persiste el voto.
- `update(id, VotingDto)` → permite modificar el score de un voto existente (intervención manual del organizador). Score = 0 es válido (anular voto).
- `delete(id)` → elimina un voto.

---

### `EventParticipationService`
**Se preocupa de:** quién participa en qué categoría de qué evento y con qué rol.
**Llama a:** `EventParticipationRepository`, `EventRepository`, `UserRepository`, `CategoryRepository`.
**Operaciones clave:**
- `registerParticipation(eventId, userId, categoryId, role)` → valida que la categoría pertenece al evento, que no haya duplicado `(event, user, category)`, y persiste la participación.
- `registerCompetitor(...)` / `registerVoter(...)` → wrappers del método anterior con rol fijado.
- `getParticipationsByEventAndCategory(eventId, categoryId)` → lista participaciones de una categoría.
- `getCompetitorsByEventAndCategory(...)` / `getVotersByEventAndCategory(...)` → filtra por rol.
- `removeParticipation(eventId, userId, categoryId)` → da de baja a un participante.

---

### `UserService`, `ParticipantService`, `CompetitorService`, `VoterService`, `TimeWindowService`
CRUD estándar para sus respectivas entidades. Sin lógica de negocio compleja en Sprint 0.

---

## 5. Capa de Controllers (`controller/`)

Exponen los servicios como endpoints REST (`@RestController`). Reciben DTOs en el body (`@RequestBody`) o como path/query params. Delegan toda la lógica al servicio correspondiente.

| Controller | Servicio que usa | Ruta base |
|---|---|---|
| `EventController` | `EventService` | `/events` |
| `CategoryController` | `CategoryService` | `/categories` |
| `CriterionController` | `CriterionService` | `/criteria` |
| `ProjectController` | `ProjectService` | `/projects` |
| `CompetitorController` | `CompetitorService` | `/competitors` |
| `VoterController` | `VoterService` | `/voters` |
| `VotingController` | `VotingService` | `/votings` |
| `ParticipantController` | `ParticipantService` | `/participants` |
| `UserController` | `UserService` | `/users` |
| `TimeWindowController` | `TimeWindowService` | `/time-windows` |

---

## 6. Diagrama de dependencias entre servicios

```
EventService
  ├── EventRepository
  ├── UserRepository
  └── EventParticipationService ──┐
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
  └── CriterionRepository

ProjectService
  ├── ProjectRepository
  ├── EventRepository
  ├── CompetitorRepository
  ├── VoterRepository
  └── CommentRepository

CriterionService
  └── CriterionRepository
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

## 8. Tests

| Clase | Tipo | Tests | Qué valida |
|---|---|---|---|
| `CriterionServiceTest` | Unitario | 9 | CRUD criterios |
| `EventServiceTest` | Unitario | 9 | CRUD eventos, organizador |
| `CategoryServiceTest` | Unitario | 16 | CRUD, tipos voto, puntos 100%, fechas |
| `VotingServiceTest` | Unitario | 10 | Votos, intervención manual, score=0 |
| `ProjectServiceTest` | Unitario | 9 | Proyectos, comentarios, competidores |
| `EventParticipationServiceTest` | Unitario | 8 | Registro, duplicados, validación categoría |
| `CategoryRepositoryTest` | Integración | 9 | Queries JPA en H2 |
| `VotingRepositoryTest` | Integración | 8 | Persistencia votos en H2 |
| `EventParticipationRepositoryTest` | Integración | 8 | Queries complejas participaciones |
| **Total** | | **86** | |

Configuración: tests unitarios usan **Mockito** (sin contexto Spring); tests de integración usan **@DataJpaTest + H2** en memoria con `MODE=PostgreSQL`.
