# Guía de Tests — Votify Backend

> **Total: 83 tests** distribuidos en 6 clases de servicio (unit tests) y 3 clases de repositorio (integration tests).
>
> - **Tests unitarios** → usan Mockito para simular dependencias. No arrancan ningún contexto de Spring ni base de datos real.
> - **Tests de integración** → usan `@DataJpaTest` con H2 en memoria. Levantan el contexto JPA real y ejecutan queries contra una BD temporal que se reinicia entre cada test.

---

## Índice

1. [CriterionServiceTest](#1-criterionservicetest--9-tests)
2. [EventServiceTest](#2-eventservicetest--9-tests)
3. [CategoryServiceTest](#3-categoryservicetest--16-tests)
4. [VotingServiceTest](#4-votingservicetest--10-tests)
5. [ProjectServiceTest](#5-projectservicetest--9-tests)
6. [EventParticipationServiceTest](#6-eventparticipationservicetest--8-tests)
7. [CategoryRepositoryTest](#7-categoryrepositorytest--9-tests)
8. [VotingRepositoryTest](#8-votingrepositorytest--8-tests)
9. [EventParticipationRepositoryTest](#9-eventparticipationrepositorytest--8-tests)

---

## 1. CriterionServiceTest — 9 tests

**Tipo:** Unitario | **Clase bajo prueba:** `CriterionService`
**Dependencia simulada:** `CriterionRepository` (Mockito)

Los criterios son los parámetros de evaluación que se usan para puntuar proyectos (ej. "Innovación", "Calidad Técnica"). Esta clase prueba que el servicio gestiona correctamente su ciclo de vida CRUD.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsListOfDtos` | Que `findAll()` convierte correctamente las entidades en DTOs y devuelve todos. | Simula el repositorio devolviendo 2 criterios. Verifica que el resultado tiene 2 elementos y que los nombres coinciden. |
| 2 | `findAll_returnsEmptyList_whenNoCriteria` | Que `findAll()` devuelve una lista vacía cuando no hay criterios. | Simula el repositorio devolviendo una lista vacía. Verifica que el resultado también está vacío. |
| 3 | `findById_returnsDto_whenFound` | Que `findById()` devuelve el DTO correcto cuando el criterio existe. | Simula `findById(1L)` devolviendo el criterio con id=1. Verifica id y nombre en el resultado. |
| 4 | `findById_throwsException_whenNotFound` | Que `findById()` lanza `RuntimeException` con el id en el mensaje cuando no existe. | Simula `findById(99L)` devolviendo `Optional.empty()`. Usa `assertThatThrownBy` para verificar la excepción y que el mensaje contiene "99". |
| 5 | `create_savesAndReturnsDto` | Que `create()` guarda la entidad y devuelve el DTO con el id generado. | Simula `save()` devolviendo la entidad con id=3. Verifica que el DTO resultante tiene ese id y el nombre correcto. |
| 6 | `create_callsSaveOnce` | Que `create()` llama exactamente una vez al repositorio. | Llama a `create()` y usa `verify(..., times(1)).save(any())` para confirmar que no hay llamadas duplicadas. |
| 7 | `update_changesNameAndReturnUpdatedDto` | Que `update()` localiza el criterio, aplica el nuevo nombre y devuelve el DTO actualizado. | Simula `findById` y `save`. Verifica que el nombre en el resultado es el nuevo. |
| 8 | `update_throwsException_whenNotFound` | Que `update()` lanza excepción si el criterio a actualizar no existe. | Simula `findById(99L)` devolviendo vacío y verifica que se lanza `RuntimeException`. |
| 9 | `delete_callsDeleteById` | Que `delete()` delega exactamente en `deleteById()` del repositorio con el id correcto. | Llama a `delete(1L)` y verifica con `verify` que `deleteById(1L)` fue llamado una vez. |

---

## 2. EventServiceTest — 9 tests

**Tipo:** Unitario | **Clase bajo prueba:** `EventService`
**Dependencias simuladas:** `EventRepository`, `UserRepository`, `EventParticipationService`

Los eventos son el núcleo de Votify (un hackathon, un demo day, etc.). Esta clase prueba la lógica de negocio para crear, buscar, actualizar y eliminar eventos.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsAllEventsAsDtos` | Que `findAll()` devuelve todos los eventos como DTOs con los nombres correctos. | Simula el repositorio con 2 eventos. Verifica tamaño y nombres exactos en el resultado. |
| 2 | `findAll_returnsEmptyList_whenNoEvents` | Que `findAll()` devuelve vacío si no hay eventos. | Simula lista vacía. Verifica resultado vacío. |
| 3 | `findById_returnsCorrectDto` | Que `findById()` devuelve el DTO del evento correcto. | Simula `findById(1L)` y verifica id y nombre en el resultado. |
| 4 | `findById_throwsException_whenNotFound` | Que `findById()` lanza excepción con el id en el mensaje si no existe. | Simula `Optional.empty()` y verifica la excepción. |
| 5 | `createForOrganizer_createsEventWithOrganizer` | Que `createForOrganizer()` asocia el organizador correcto al evento y lo persiste. | Simula la búsqueda del usuario y el `save`. Verifica id, nombre y que se llamó a `save` una vez. |
| 6 | `createForOrganizer_throwsException_whenOrganizerNotFound` | Que si el organizador no existe se lanza excepción con su id en el mensaje. | Simula `userRepository.findById(999L)` devolviendo vacío. Verifica excepción. |
| 7 | `update_updatesEventNameAndDates` | Que `update()` aplica los nuevos datos al evento y devuelve el DTO actualizado. | Simula `findById` y `save`. Verifica el nuevo nombre en el resultado. |
| 8 | `delete_callsDeleteByIdOnce` | Que `delete()` llama exactamente una vez a `deleteById()`. | Configura `doNothing()` sobre `deleteById(1L)` y verifica la llamada con `verify`. |
| 9 | `delete_doesNotThrow_whenNotFound` | Que `delete()` no lanza excepción aunque el id no exista (comportamiento de Spring Data). | Usa `assertThatCode(...).doesNotThrowAnyException()` para confirmar que es silencioso para ids inexistentes. |

---

## 3. CategoryServiceTest — 16 tests

**Tipo:** Unitario | **Clase bajo prueba:** `CategoryService`
**Dependencias simuladas:** `CategoryRepository`, `EventRepository`, `CriterionRepository`, `CategoryCriterionPointsRepository`

Las categorías agrupan a participantes bajo un tipo de votación dentro de un evento (ej. "Jurado Experto", "Voto Popular"). Esta clase es la más extensa porque también gestiona la asignación de tipos de voto, puntos por criterio y validación de fechas.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsAllCategories` | Que `findAll()` devuelve todas las categorías como DTOs. | Simula 2 categorías en el repositorio y verifica nombres en el resultado. |
| 2 | `findByEventId_returnsOnlyCategoriesOfEvent` | Que `findByEventId()` filtra solo las categorías del evento indicado. | Simula 1 categoría para el evento id=1 y verifica que el eventId es correcto. |
| 3 | `findByEventId_returnsEmpty_whenNoCategoriesForEvent` | Que devuelve vacío para un evento sin categorías. | Simula lista vacía y verifica resultado vacío. |
| 4 | `findById_returnsDto` | Que `findById()` devuelve el DTO correcto con id y nombre. | Simula `findById(10L)` y verifica el resultado. |
| 5 | `findById_throwsException_whenNotFound` | Que `findById()` lanza excepción con el id si no existe. | Simula `Optional.empty()` y verifica excepción. |
| 6 | `createForEvent_createsCategoryLinkedToEvent` | Que `createForEvent()` crea la categoría enlazada al evento correcto. | Simula `findById` del evento y `save`. Verifica id, nombre y eventId en el resultado. |
| 7 | `createForEvent_throwsException_whenEventNotFound` | Que lanza excepción si el evento no existe al crear la categoría. | Simula `Optional.empty()` y verifica excepción con id "99". |
| 8 | `setVotingType_setsJuryExpert` | Que `setVotingType()` asigna correctamente el tipo `JURY_EXPERT`. | Simula repositorio y verifica que el DTO devuelve `JURY_EXPERT`. |
| 9 | `setVotingType_setsPopularVote` | Que `setVotingType()` asigna correctamente el tipo `POPULAR_VOTE`. | Igual que el anterior pero con `POPULAR_VOTE`. |
| 10 | `setCriterionPointsBulk_savesPointsForEachCriterion` | Que `setCriterionPointsBulk()` guarda correctamente cuando la suma de los valores de los criterios es exactamente 100. | Simula la BD y verifica que se borraron los datos anteriores y se guardaron los nuevos asegurando que sumán 100. |
| 11 | `setCriterionPointsBulk_throwsException_whenNegativePoints` | Que no se permiten puntos negativos en un criterio. | Pasa `maxPoints = -5` y verifica que se lanza `RuntimeException` con mensaje de validación. |
| 12 | `setCriterionPointsBulk_throwsException_whenSumIsNot100` | Que la suma de puntos *bulk* de una categoría debe ser exactamente 100%. | Pasa una lista de puntos que suma 80 (ej. 40+40) y verifica que lanza `RuntimeException` indicando "100". |
| 13 | `setCriterionPoints_throwsException_whenExceeds100` | Que la actualización de puntos de un criterio singular (por slider) no debe superar el 100%. | Intenta aplicar 50 puntos sobre un criterio, cuando el resto suman 70; verifica que lanza error porque 70+50 = 120 > 100. |
| 14 | `setTimeInitial_throwsException_whenBeforeEventStart` | Que la fecha de inicio de la categoría no puede ser anterior al inicio del evento. | Configura el evento con inicio en 5000ms y pasa fecha 1ms. Verifica excepción con "start time". |
| 15 | `setTimeFinal_throwsException_whenEndBeforeStart` | Que la fecha de fin de la categoría no puede ser anterior a su fecha de inicio. | Configura la categoría con inicio en 5000ms y pasa fecha fin 1ms. Verifica excepción con "end time". |

---

## 4. VotingServiceTest — 10 tests

**Tipo:** Unitario | **Clase bajo prueba:** `VotingService`
**Dependencias simuladas:** `VotingRepository`, `VoterRepository`, `CompetitorRepository`, `CriterionRepository`

Los votos son la acción central de Votify: un jurado puntúa un proyecto según un criterio. Esta clase también cubre la funcionalidad de **intervención manual**, que permite al organizador corregir o anular puntuaciones.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsAllVotings` | Que `findAll()` devuelve todos los votos con sus puntuaciones. | Simula 2 votos (25 y 18 pts). Verifica tamaño y que las puntuaciones están presentes. |
| 2 | `findById_returnsDto_whenFound` | Que `findById()` devuelve el voto con voterId, competitorId, criterionId y score correctos. | Simula `findById(100L)` y verifica los 4 campos del DTO. |
| 3 | `findById_throwsException_whenNotFound` | Que lanza excepción con el id si el voto no existe. | Simula `Optional.empty()` y verifica excepción. |
| 4 | `create_savesVotingWithCorrectEntities` | Que `create()` resuelve las 3 entidades (votante, competidor, criterio) y persiste el voto. | Simula los 3 repositorios y `save`. Verifica puntuación y que se llamó a `save` una vez. |
| 5 | `create_throwsException_whenVoterNotFound` | Que lanza excepción con "Voter" si el votante no existe. | Simula `voterRepository.findById(99L)` vacío y verifica excepción. |
| 6 | `create_throwsException_whenCompetitorNotFound` | Que lanza excepción con "Competitor" si el competidor no existe. | Simula voter correcto pero competitor vacío. Verifica excepción. |
| 7 | `create_throwsException_whenCriterionNotFound` | Que lanza excepción con "Criterion" si el criterio no existe. | Simula voter y competitor correctos pero criterion vacío. Verifica excepción. |
| 8 | `update_changesScore` | Que `update()` modifica la puntuación de un voto existente (intervención manual). | Simula las 4 entidades y `save` devolviendo score=30. Verifica que el resultado es 30. |
| 9 | `delete_callsDeleteById` | Que `delete()` elimina el voto llamando a `deleteById` con el id correcto. | Verifica con `verify` que `deleteById(100L)` se llama una vez. |
| 10 | `update_allowsScoreZero` | Que la intervención manual permite poner la puntuación a 0 (anular un voto). | Simula `update` con score=0. Verifica que el resultado devuelve 0 sin errores. |

---

## 5. ProjectServiceTest — 9 tests

**Tipo:** Unitario | **Clase bajo prueba:** `ProjectService`
**Dependencias simuladas:** `ProjectRepository`, `EventRepository`, `CompetitorRepository`, `VoterRepository`, `CommentRepository`

Los proyectos son los competidores del evento (equipos/trabajos a evaluar). Esta clase prueba la gestión de proyectos y dos operaciones relacionadas: añadir comentarios y asociar competidores.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findByEvent_returnsProjectsOfEvent` | Que `findByEvent()` devuelve los proyectos del evento con sus nombres. | Simula 2 proyectos en el repositorio. Verifica tamaño y nombres. |
| 2 | `findByEvent_returnsEmpty_whenNoProjects` | Que devuelve vacío cuando el evento no tiene proyectos. | Simula lista vacía y verifica el resultado. |
| 3 | `createForEvent_createsProject` | Que `createForEvent()` crea el proyecto enlazado al evento y lo persiste. | Simula `findById` del evento y `save`. Verifica id, nombre y que `save` fue llamado una vez. |
| 4 | `createForEvent_throwsException_whenEventNotFound` | Que lanza excepción con el id si el evento no existe. | Simula `Optional.empty()` y verifica excepción. |
| 5 | `addComment_savesComment` | Que `addComment()` guarda el comentario con texto, voterId e id correcto. | Simula proyecto, voter y `commentRepository.save`. Verifica los 3 campos del DTO resultante y que `save` fue llamado. |
| 6 | `addComment_throwsException_whenProjectNotFound` | Que lanza excepción si el proyecto destinatario no existe. | Simula `projectRepository.findById(99L)` vacío. Verifica excepción. |
| 7 | `addComment_throwsException_whenVoterNotFound` | Que lanza excepción con "Voter" si el autor del comentario no existe. | Simula proyecto correcto pero voter vacío. Verifica excepción. |
| 8 | `addCompetitor_linksCompetitorToProject` | Que `addCompetitor()` añade el competidor a la lista del proyecto y lo persiste. | Simula proyecto y competidor. Verifica que `project.getCompetitors()` contiene el competidor y que `save` fue llamado. |
| 9 | `addCompetitor_throwsException_whenCompetitorNotFound` | Que lanza excepción con "Competitor" si el competidor no existe. | Simula `competitorRepository.findById(99L)` vacío y verifica excepción. |

---

## 6. EventParticipationServiceTest — 8 tests

**Tipo:** Unitario | **Clase bajo prueba:** `EventParticipationService`
**Dependencias simuladas:** `EventParticipationRepository`, `EventRepository`, `UserRepository`, `CategoryRepository`

Las participaciones definen quién participa en cada evento, en qué categoría y con qué rol (competidor o votante). Esta clase prueba el registro, consulta y baja de participantes.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `registerParticipation_registersCompetitor` | Que `registerCompetitor()` crea una participación con rol `COMPETITOR` y la persiste. | Simula evento, usuario, categoría y `existsBy` = false. Verifica rol en el resultado y que `save` fue llamado. |
| 2 | `registerParticipation_registersVoter` | Que `registerVoter()` crea una participación con rol `VOTER`. | Igual que el anterior pero para rol `VOTER`. |
| 3 | `registerParticipation_throwsException_whenCategoryIsNull` | Que no se puede registrar una participación sin especificar categoría. | Llama a `registerParticipation` con `categoryId = null`. Verifica excepción con "Category is required". |
| 4 | `registerParticipation_throwsException_whenAlreadyRegistered` | Que no se puede registrar dos veces al mismo usuario en la misma categoría del evento. | Simula `existsByEventIdAndUserIdAndCategoryId` devolviendo `true`. Verifica excepción con "already registered". |
| 5 | `registerParticipation_throwsException_whenCategoryDoesNotBelongToEvent` | Que no se puede registrar en una categoría de otro evento. | Simula una categoría cuyo `event.id` es diferente al evento destino. Verifica excepción con "belong". |
| 6 | `getParticipationsByEvent_returnsParticipations` | Que `getParticipationsByEvent()` devuelve todas las participaciones del evento con el rol correcto. | Simula 1 participación con rol `COMPETITOR`. Verifica tamaño y rol. |
| 7 | `removeParticipation_deletesParticipation` | Que `removeParticipation()` localiza y elimina la participación correcta. | Simula la búsqueda y `delete`. Verifica que `delete(p)` fue llamado una vez. |
| 8 | `removeParticipation_throwsException_whenNotFound` | Que lanza excepción si la participación a eliminar no existe. | Simula búsqueda devolviendo `Optional.empty()` y verifica excepción. |

---

## 7. CategoryRepositoryTest — 9 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `CategoryRepository`
**Base de datos:** H2 en memoria con modo PostgreSQL

A diferencia de los tests unitarios, estos tests levantan el contexto JPA real. Se usan para verificar que las queries derivadas del nombre del método funcionan correctamente contra una base de datos real. Cada test parte de 3 categorías precargadas: `cat1` y `cat2` en `event1`, y `cat3` en `event2`.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findByEventId_returnsOnlyCategoriesOfGivenEvent` | Que `findByEventId(event1.id)` devuelve exactamente las 2 categorías de `event1`. | Ejecuta la query y verifica tamaño y nombres. |
| 2 | `findByEventId_doesNotIncludeCategoriesFromOtherEvents` | Que `findByEventId` no filtra categorías de otros eventos. | Verifica que `cat3` ("Otra Categoría") no aparece en el resultado de `event1`. |
| 3 | `findByEventId_returnsEmpty_forEventWithNoCategories` | Que devuelve vacío para un evento recién creado sin categorías. | Persiste un evento nuevo y verifica que su lista es vacía. |
| 4 | `findById_returnsCorrectCategory` | Que `findById` devuelve la categoría correcta con su `VotingType`. | Busca `cat1` por su id. Verifica nombre y que el tipo es `JURY_EXPERT`. |
| 5 | `findById_returnsEmpty_whenNotFound` | Que `findById` devuelve vacío para un id inexistente. | Busca id `9999L`. Verifica que el Optional está vacío. |
| 6 | `save_persistsNewCategory` | Que `save` genera un id y persiste la nueva categoría de forma recuperable. | Guarda una nueva categoría y verifica que tiene id no nulo y que se puede recuperar con `findById`. |
| 7 | `save_updatesExistingCategory` | Que `save` sobre una categoría existente actualiza sus datos en la BD. | Cambia el `VotingType` de `cat1` a `POPULAR_VOTE`, guarda y recarga con `em.find`. Verifica el nuevo tipo. |
| 8 | `deleteById_removesCategory` | Que `deleteById` elimina la categoría de la BD. | Elimina `cat1`, hace flush/clear y verifica que `findById` devuelve vacío. |
| 9 | `findAll_returnsAllPersistedCategories` | Que `findAll` devuelve al menos las 3 categorías insertadas en el setup. | Llama a `findAll()` y verifica que hay 3 o más resultados. |

---

## 8. VotingRepositoryTest — 8 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `VotingRepository`
**Base de datos:** H2 en memoria con modo PostgreSQL

Prueba la persistencia de votos en la BD real, incluyendo que las relaciones con `Voter`, `Competitor` y `Criterion` se cargan correctamente, y que la edición de puntuaciones (intervención manual) persiste. El setup inserta 2 votos del mismo jurado sobre el mismo competidor con criterios distintos (25 pts y 18 pts).

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsAllVotings` | Que `findAll` devuelve al menos los 2 votos insertados en el setup. | Llama a `findAll()` y verifica que hay 2 o más resultados. |
| 2 | `findById_returnsVotingWithRelations` | Que `findById` recupera el voto con todas sus relaciones (voter, competitor, criterion) resueltas. | Busca `voting1` y verifica los 4 campos: score, voterId, competitorId, criterionId. |
| 3 | `findById_returnsEmpty_whenNotFound` | Que `findById` devuelve vacío para un id inexistente. | Busca id `99999L` y verifica Optional vacío. |
| 4 | `save_persistsNewVoting` | Que `save` genera id y persiste un nuevo voto recuperable. | Guarda voto nuevo, limpia caché con `em.clear()` y lo recarga. Verifica score=30. |
| 5 | `save_updatesScore_whenEdited` | Que modificar el score y hacer `save` actualiza el dato en la BD (intervención manual). | Cambia `voting1.score` a 40, guarda, hace flush/clear y verifica con `em.find`. |
| 6 | `deleteById_removesVoting` | Que `deleteById` elimina el voto de la BD. | Elimina `voting1`, hace flush/clear y verifica que `findById` devuelve vacío. |
| 7 | `deleteById_doesNotAffectOtherVotings` | Que eliminar un voto no afecta a otros votos del mismo jurado. | Elimina `voting1` y verifica que `voting2` sigue presente. |
| 8 | `count_returnsCorrectTotal` | Que `count()` cuenta correctamente el total de votos en la BD. | Verifica que el resultado es mayor o igual a 2. |

---

## 9. EventParticipationRepositoryTest — 8 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `EventParticipationRepository`
**Base de datos:** H2 en memoria con modo PostgreSQL

Esta clase prueba las queries personalizadas del repositorio de participaciones. Es la más compleja porque el repositorio expone múltiples métodos de búsqueda combinando evento, usuario, categoría y rol. El setup crea un evento con 2 categorías y 3 participaciones: `userComp` en `catJury` (COMPETITOR), `userVoter` en `catJury` (VOTER), y `userComp` en `catPopular` (COMPETITOR).

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findByEventId_returnsAllParticipationsOfEvent` | Que `findByEventId` devuelve las 3 participaciones del evento. | Llama a la query y verifica tamaño = 3. |
| 2 | `findByEventIdAndCategoryId_filtersCorrectly` | Que `findByEventIdAndCategoryId` devuelve solo las 2 participaciones de `catJury`. | Filtra por evento y `catJury`. Verifica tamaño = 2. |
| 3 | `findByEventIdAndCategoryIdAndRole_returnsOnlyCompetitors` | Que filtrando además por rol `COMPETITOR` en `catJury` devuelve solo `userComp`. | Filtra con rol `COMPETITOR`. Verifica tamaño = 1 y que el usuario es `userComp`. |
| 4 | `findByEventIdAndCategoryIdAndRole_returnsOnlyVoters` | Que filtrando por rol `VOTER` en `catJury` devuelve solo `userVoter`. | Filtra con rol `VOTER`. Verifica tamaño = 1 y que el usuario es `userVoter`. |
| 5 | `exists_returnsTrue_whenAlreadyRegistered` | Que `existsByEventIdAndUserIdAndCategoryId` devuelve `true` para una combinación que existe. | Comprueba `userComp` en `catJury` del evento. Verifica `true`. |
| 6 | `exists_returnsFalse_whenNotRegistered` | Que devuelve `false` para una combinación que no existe. | Comprueba `userVoter` en `catPopular` (donde no está inscrito). Verifica `false`. |
| 7 | `findByEventAndUserAndCategory_returnsCorrectParticipation` | Que `findByEventIdAndUserIdAndCategoryId` devuelve exactamente la participación correcta. | Busca `userComp` en `catJury`. Verifica que está presente y que el rol es `COMPETITOR`. |
| 8 | `findByUserId_returnsAllParticipationsOfUser` | Que `findByUserId` devuelve todas las participaciones de un usuario en todos sus eventos/categorías. | Busca participaciones de `userComp` (que está en 2 categorías). Verifica tamaño = 2. |

---

## Resumen por clase

| Clase | Tipo | Tests | Cubre |
|-------|------|-------|-------|
| `CriterionServiceTest` | Unitario | 9 | CRUD de criterios de evaluación |
| `EventServiceTest` | Unitario | 9 | CRUD de eventos, creación con organizador |
| `CategoryServiceTest` | Unitario | 16 | CRUD, tipos de voto, validación 100% de puntos, validación de fechas |
| `VotingServiceTest` | Unitario | 10 | Registro de votos, validación de entidades, intervención manual |
| `ProjectServiceTest` | Unitario | 9 | CRUD de proyectos, comentarios, asociación de competidores |
| `EventParticipationServiceTest` | Unitario | 8 | Registro de participantes, duplicados, validación de categoría |
| `CategoryRepositoryTest` | Integración | 9 | Queries JPA sobre categorías en BD real |
| `VotingRepositoryTest` | Integración | 8 | Persistencia de votos, relaciones, intervención manual en BD |
| `EventParticipationRepositoryTest` | Integración | 8 | Queries complejas de participaciones por evento, categoría y rol |
| **Total** | | **86** | |
