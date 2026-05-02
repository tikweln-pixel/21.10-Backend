# Guía de Tests — Votify Backend

> **Total: 203 tests** distribuidos en 10 clases de servicio (unit tests), 1 clase de factory (unit test) y 6 clases de repositorio (integration tests).
>
> - **Tests unitarios** → usan Mockito para simular dependencias. No arrancan ningún contexto de Spring ni base de datos real.
> - **Tests de integración** → usan `@DataJpaTest` con H2 en memoria. Levantan el contexto JPA real y ejecutan queries contra una BD temporal que se reinicia entre cada test.

---

## Índice

**Servicio (unitarios — Mockito)**
1. [CriterionServiceTest](#1-criterionservicetest--14-tests) — 14 tests
2. [EventServiceTest](#2-eventservicetest--9-tests) — 9 tests
3. [CategoryServiceTest](#3-categoryservicetest--25-tests) — 25 tests
4. [VotingServiceTest](#4-votingservicetest--28-tests) — 28 tests
5. [ProjectServiceTest](#5-projectservicetest--9-tests) — 9 tests
6. [EventParticipationServiceTest](#6-eventparticipationservicetest--17-tests) — 17 tests
7. [EvaluacionServiceTest](#7-evaluacionservicetest--12-tests) — 12 tests
8. [UserServiceTest](#8-userservicetest--15-tests) — 15 tests
9. [HojaRutaMejoraServiceTest](#9-hojarutamejoraservicetest--13-tests) — 13 tests

**Factory (unitarios — Mockito)**

10. [EvaluacionCreatorTest](#10-evaluacioncreatortest--17-tests) — 17 tests

**Repositorio (integración — @DataJpaTest + H2)**

11. [CategoryRepositoryTest](#11-categoryrepositorytest--9-tests) — 9 tests
12. [VotingRepositoryTest](#12-votingrepositorytest--10-tests) — 10 tests
13. [EventParticipationRepositoryTest](#13-eventparticipationrepositorytest--8-tests) — 8 tests
14. [EventRepositoryTest](#14-eventrepositorytest--5-tests) — 5 tests
15. [ProjectRepositoryTest](#15-projectrepositorytest--6-tests) — 6 tests
16. [EvaluacionRepositoryTest](#16-evaluacionrepositorytest--6-tests) — 6 tests

---

## 1. CriterionServiceTest — 14 tests

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
| 10–14 | *(tests adicionales Sprint 2/3)* | Validaciones extendidas y casos borde de CRUD de criterios. | Ver código fuente `CriterionServiceTest.java`. |

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

## 3. CategoryServiceTest — 25 tests

**Tipo:** Unitario | **Clase bajo prueba:** `CategoryService`
**Dependencias simuladas:** `CategoryRepository`, `EventRepository`, `CriterionRepository`, `CategoryCriterionPointsRepository`, `EventParticipationRepository`, `UserRepository`

Las categorías agrupan a participantes bajo un tipo de votación dentro de un evento (ej. "Jurado Experto", "Voto Popular"). Esta clase es la más extensa porque también gestiona la asignación de tipos de voto, puntos por criterio, límites de votación popular y validación de fechas.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsAllCategories` | Que `findAll()` devuelve todas las categorías como DTOs. | Simula 2 categorías en el repositorio y verifica nombres en el resultado. |
| 2 | `findByEventId_returnsOnlyCategoriesOfEvent` | Que `findByEventId()` filtra solo las categorías del evento indicado. | Simula 1 categoría para el evento id=1 y verifica que el eventId es correcto. |
| 3 | `findByEventId_returnsEmpty_whenNoCategoriesForEvent` | Que devuelve vacío para un evento sin categorías. | Simula lista vacía y verifica resultado vacío. |
| 4 | `findById_returnsDto` | Que `findById()` devuelve el DTO correcto con id y nombre. | Simula `findById(10L)` y verifica el resultado. |
| 5 | `findById_throwsException_whenNotFound` | Que `findById()` lanza excepción con el id si no existe. | Simula `Optional.empty()` y verifica excepción. |
| 6 | `createForEvent_createsCategoryLinkedToEvent` | Que `createForEvent()` crea la categoría enlazada al evento correcto. | Simula `findById` del evento y `save`. Verifica id, nombre y eventId en el resultado. |
| 7 | `createForEvent_registersSpectatorsForExistingEventUsers` | Que al crear categoría se registran automáticamente los usuarios del evento como espectadores. | Verifica que `ensureSpectatorRegistration` es llamado con los usuarios del evento. |
| 8 | `createForEvent_throwsException_whenEventNotFound` | Que lanza excepción si el evento no existe al crear la categoría. | Simula `Optional.empty()` y verifica excepción con id "99". |
| 9 | `setVotingType_setsJuryExpert` | Que `setVotingType()` asigna correctamente el tipo `JURY_EXPERT`. | Simula repositorio y verifica que el DTO devuelve `JURY_EXPERT`. |
| 10 | `setVotingType_setsPopularVote` | Que `setVotingType()` asigna correctamente el tipo `POPULAR_VOTE`. | Igual que el anterior pero con `POPULAR_VOTE`. |
| 11 | `setCriterionPointsBulk_savesPointsForEachCriterion` | Que `setCriterionPointsBulk()` guarda correctamente cuando la suma es exactamente 100. | Simula la BD y verifica que se borraron los datos anteriores y se guardaron los nuevos. |
| 12 | `setCriterionPointsBulk_throwsException_whenNegativePoints` | Que no se permiten puntos negativos en un criterio. | Pasa `maxPoints = -5` y verifica que se lanza `RuntimeException`. |
| 13 | `setCriterionPointsBulk_throwsException_whenSumIsNot100` | Que la suma de puntos bulk debe ser exactamente 100%. | Pasa una lista que suma 80 y verifica excepción con "100". |
| 14 | `setCriterionPointsBulk_savesPoints_whenSumIsExactly100` | Que funciona cuando la suma es exactamente 100. | Pasa lista que suma 100 exacto. Verifica que `save` es llamado. |
| 15 | `setCriterionPoints_throwsException_whenExceeds100` | Que la actualización singular no puede superar el 100%. | Intenta 50 pts cuando el resto suma 70. Verifica excepción. |
| 16 | `setTimeInitial_throwsException_whenBeforeEventStart` | Que la fecha de inicio de la categoría no puede ser anterior al inicio del evento. | Configura evento con inicio en 5000ms y pasa fecha 1ms. Verifica excepción. |
| 17 | `setTimeFinal_throwsException_whenEndBeforeStart` | Que la fecha de fin no puede ser anterior al inicio de la categoría. | Configura inicio en 5000ms y pasa fecha fin 1ms. Verifica excepción. |
| 18 | `setTotalPoints_setsPointsForPopularVote` | Que `setTotalPoints()` asigna el límite de puntos para voto popular. | Solo es válido para categorías `POPULAR_VOTE`. Verifica el valor guardado. |
| 19 | `setTotalPoints_throwsException_whenJuryExpert` | Que no se puede establecer totalPoints en categorías de tipo `JURY_EXPERT`. | Verifica excepción cuando el tipo es jurado. |
| 20 | `setTotalPoints_throwsException_whenZeroOrNegative` | Que totalPoints debe ser positivo. | Pasa 0 o negativo y verifica excepción. |
| 21 | `setMaxVotesPerVoter_setsLimitForPopularVote` | Que `setMaxVotesPerVoter()` asigna el límite de proyectos distintos que puede votar un votante. | Verifica el valor guardado para `POPULAR_VOTE`. |
| 22 | `setMaxVotesPerVoter_throwsException_whenJuryExpert` | Que no se puede establecer este límite en categorías de tipo `JURY_EXPERT`. | Verifica excepción cuando el tipo es jurado. |
| 23 | `setMaxVotesPerVoter_throwsException_whenZeroOrNegative` | Que el límite debe ser positivo. | Pasa 0 o negativo y verifica excepción. |
| 24 | `delete_unlinksProjectsAndDeletesCriteriaBeforeDeletingCategory` | Que al borrar una categoría se desvinculan proyectos y criterios antes de eliminarla. | Verifica el orden de operaciones con múltiples `verify`. |
| 25 | `setCriterionPointsBulk_worksForPopularVote` | Que la asignación bulk de puntos también funciona para categorías de voto popular. | Verifica que el flujo completo es válido para `POPULAR_VOTE`. |

---

## 4. VotingServiceTest — 28 tests

**Tipo:** Unitario | **Clase bajo prueba:** `VotingService`
**Dependencias simuladas:** `VotingRepository`, `VoterRepository`, `ProjectRepository`, `CriterionRepository`, `CategoryRepository`, `EventParticipationRepository`

Los votos son la acción central de Votify. Esta clase cubre: registro de votos (jurado y popular), validaciones de periodo de votación, límites de voto popular, auto-voto, duplicados, y la **intervención manual** del organizador.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsAllVotings` | Que `findAll()` devuelve todos los votos con sus puntuaciones. | Simula 2 votos. Verifica tamaño y puntuaciones. |
| 2 | `findById_returnsDto_whenFound` | Que `findById()` devuelve el voto con voterId, projectId, criterionId y score correctos. | Simula `findById(100L)` y verifica los 4 campos. |
| 3 | `findById_throwsException_whenNotFound` | Que lanza excepción con el id si el voto no existe. | Simula `Optional.empty()` y verifica excepción. |
| 4 | `create_savesVotingWithCorrectEntities` | Que `create()` resuelve las entidades (voter, project, criterion) y persiste el voto. | Simula los 3 repositorios y `save`. Verifica puntuación y que se llamó a `save` una vez. |
| 5 | `create_appliesCriterionWeightingForWeightedScore` | Que la puntuación se pondera según el peso del criterio en la categoría. | Simula un criterio con 40% de peso y verifica que el score guardado es el correcto. |
| 6 | `create_throwsException_whenVoterNotFound` | Que lanza excepción con "Voter" si el votante no existe. | Simula `voterRepository` devolviendo vacío. Verifica excepción. |
| 7 | `create_throwsException_whenProjectNotFound` | Que lanza excepción si el proyecto no existe. | Simula `projectRepository` devolviendo vacío. Verifica excepción. |
| 8 | `create_throwsException_whenProjectHasNoCompetitors` | Que lanza excepción si el proyecto no tiene competidores asociados. | Simula proyecto sin competidores. Verifica excepción. |
| 9 | `create_throwsException_whenVoterIsCompetitorOfProject` | Que un competidor no puede votar su propio proyecto (auto-voto). | Simula que el voterId coincide con un competidor del proyecto. Verifica excepción. |
| 10 | `create_throwsException_whenCriterionNotFound` | Que lanza excepción si el criterio no existe. | Simula criterion vacío. Verifica excepción. |
| 11 | `create_throwsException_whenDuplicateVote` | Que no se puede votar dos veces al mismo proyecto con el mismo criterio. | Simula `existsByVoterIdAndProjectIdAndCriterionId` devolviendo `true`. Verifica excepción. |
| 12 | `create_neverSaves_whenDuplicateVote` | Que ante un voto duplicado, `save` nunca es invocado. | Verifica con `verify(repo, never()).save(any())` que no se persistió nada. |
| 13 | `create_neverSaves_whenVoterIsCompetitorOfProject` | Que ante auto-voto, `save` nunca es invocado. | Igual al anterior pero disparado por auto-voto. |
| 14 | `create_throwsException_whenPeriodClosed` | Que no se puede votar fuera del periodo activo (periodo cerrado). | Simula categoría con `timeFinal` en el pasado. Verifica excepción. |
| 15 | `create_throwsException_whenPeriodNotStartedYet` | Que no se puede votar antes de que empiece el periodo. | Simula categoría con `timeInitial` en el futuro. Verifica excepción. |
| 16 | `create_popularVote_allowsVote_whenBelowMaxProjects` | Que el voto popular se permite cuando el votante está por debajo del límite de proyectos distintos. | Simula historial con 1 voto de 3 permitidos. Verifica que `save` es llamado. |
| 17 | `create_popularVote_rejectsVote_whenMaxProjectsReached` | Que el voto popular es rechazado cuando el votante ya ha alcanzado el límite de proyectos distintos. | Simula historial en el límite máximo. Verifica excepción. |
| 18 | `create_popularVote_allowsRevote_whenSameProject` | Que re-votar el mismo proyecto no cuenta como proyecto nuevo (es idempotente). | Simula mismo proyecto ya votado. Verifica que no se rechaza por límite. |
| 19 | `create_popularVote_rejectsVote_whenExceedsTotalPoints` | Que el voto popular es rechazado cuando el votante ya gastó todos sus puntos. | Simula totalPoints = 10 y votos previos que suman 10. Verifica excepción. |
| 20 | `create_popularVote_allowsVote_whenNoLimitsConfigured` | Que si la categoría no tiene límites configurados, cualquier voto es permitido. | Simula categoría con `maxVotesPerVoter = null`. Verifica que `save` es llamado. |
| 21 | `update_changesScore` | Que `update()` modifica la puntuación de un voto existente (intervención manual). | Simula `findById` y `save` devolviendo score=30. Verifica que el resultado es 30. |
| 22 | `update_allowsScoreZero` | Que la intervención manual permite poner la puntuación a 0 (anular un voto). | Simula `update` con score=0. Verifica que el resultado devuelve 0 sin errores. |
| 23 | `update_recalculatesWeightedScoreUsingCriterionPercent` | Que al actualizar manualmente la puntuación se recalcula según el peso del criterio. | Verifica que el score final en el DTO incorpora la ponderación correcta. |
| 24 | `update_setsManuallyModifiedTrue_onManualIntervention` | Que al hacer intervención manual, `manuallyModified` queda en `true`. | Verifica el flag en el DTO devuelto tras `update`. |
| 25 | `update_keepsManuallyModifiedFalse_whenNotManual` | Que sin intervención manual, `manuallyModified` queda en `false`. | Verifica el flag cuando el voto es normal. |
| 26 | `delete_callsDeleteById` | Que `delete()` elimina el voto llamando a `deleteById` con el id correcto. | Verifica con `verify` que `deleteById(100L)` se llama una vez. |
| 27 | `getActiveVoterIds_returnsDistinctVoterIds_forCategory` | Que `getActiveVoterIds()` devuelve los ids únicos de votantes activos de una categoría. | Simula varios votos del mismo votante. Verifica que se deduplicar correctamente. |
| 28 | `getActiveVoterIds_returnsEmpty_whenNoVotesInCategory` | Que devuelve lista vacía si no hay votos en la categoría. | Simula lista vacía. Verifica resultado vacío. |

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
| 5 | `addComment_savesComment` | Que `addComment()` guarda el comentario con texto, voterId e id correcto. | Simula proyecto, voter y `commentRepository.save`. Verifica los 3 campos y que `save` fue llamado. |
| 6 | `addComment_throwsException_whenProjectNotFound` | Que lanza excepción si el proyecto destinatario no existe. | Simula `projectRepository.findById(99L)` vacío. Verifica excepción. |
| 7 | `addComment_throwsException_whenVoterNotFound` | Que lanza excepción con "Voter" si el autor del comentario no existe. | Simula proyecto correcto pero voter vacío. Verifica excepción. |
| 8 | `addCompetitor_linksCompetitorToProject` | Que `addCompetitor()` añade el competidor a la lista del proyecto y lo persiste. | Simula proyecto y competidor. Verifica que `project.getCompetitors()` contiene el competidor y que `save` fue llamado. |
| 9 | `addCompetitor_throwsException_whenCompetitorNotFound` | Que lanza excepción con "Competitor" si el competidor no existe. | Simula `competitorRepository.findById(99L)` vacío y verifica excepción. |

---

## 6. EventParticipationServiceTest — 17 tests

**Tipo:** Unitario | **Clase bajo prueba:** `EventParticipationService`
**Dependencias simuladas:** `EventParticipationRepository`, `EventRepository`, `UserRepository`, `CategoryRepository`

Las participaciones definen quién participa en cada evento, en qué categoría y con qué rol. En Sprint 2/3 se añadió el registro de espectadores anónimos (votantes sin cuenta previa).

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `registerParticipation_registersCompetitor` | Que `registerParticipation()` crea una participación con rol `COMPETITOR` y la persiste. | Simula evento, usuario, categoría y `existsBy = false`. Verifica rol y que `save` fue llamado. |
| 2 | `registerParticipation_registersSpectator` | Que `registerParticipation()` crea una participación con rol `SPECTATOR`. | Igual que el anterior pero para rol `SPECTATOR`. |
| 3 | `registerParticipation_throwsException_whenCategoryIsNull` | Que no se puede registrar sin especificar categoría. | Llama con `categoryId = null`. Verifica excepción con "Category is required". |
| 4 | `registerParticipation_throwsException_whenAlreadyRegistered` | Que no se puede registrar dos veces al mismo usuario en la misma categoría. | Simula `existsByEventIdAndUserIdAndCategoryId` devolviendo `true`. Verifica excepción con "already registered". |
| 5 | `registerParticipation_throwsException_whenCategoryDoesNotBelongToEvent` | Que no se puede registrar en una categoría de otro evento. | Simula categoría cuyo `event.id` es diferente. Verifica excepción con "belong". |
| 6 | `getParticipationsByEvent_returnsParticipations` | Que `getParticipationsByEvent()` devuelve todas las participaciones del evento con el rol correcto. | Simula 1 participación con rol `COMPETITOR`. Verifica tamaño y rol. |
| 7 | `removeParticipation_deletesParticipation` | Que `removeParticipation()` localiza y elimina la participación correcta. | Simula la búsqueda y `delete`. Verifica que `delete(p)` fue llamado una vez. |
| 8 | `removeParticipation_throwsException_whenNotFound` | Que lanza excepción si la participación a eliminar no existe. | Simula búsqueda devolviendo `Optional.empty()` y verifica excepción. |
| 9 | `ensureSpectatorRegistrationInAllCategories_createsMissingSpectators` | Que los espectadores que faltan en alguna categoría son registrados automáticamente. | Simula usuario sin participación en una categoría. Verifica que `save` es llamado para rellenar el hueco. |
| 10 | `ensureSpectatorRegistrationInAllCategories_keepsExistingRoles` | Que si el usuario ya tiene participación en una categoría, no se sobreescribe su rol. | Simula participación existente. Verifica que `save` no es llamado para esa categoría. |
| 11 | `registerAnonymousSpectator_createsNewUserAndRegisters` | Que un espectador anónimo (sin cuenta) es creado como usuario y registrado en el evento. | Simula email no existente. Verifica que `userRepository.save` y `participationRepository.save` son llamados. |
| 12 | `registerAnonymousSpectator_reusesExistingUserByEmail` | Que si el email ya existe, se reutiliza la cuenta en vez de crear una nueva. | Simula usuario existente con ese email. Verifica que no se crea un usuario nuevo. |
| 13 | `registerAnonymousSpectator_isIdempotent_whenAlreadyRegistered` | Que registrar dos veces al mismo espectador no produce duplicados. | Simula `existsByEventIdAndUserIdAndCategoryId = true`. Verifica que `save` no es llamado de nuevo. |
| 14 | `registerAnonymousSpectator_throwsException_whenEventIdIsNull` | Guard Clause: eventId no puede ser null. | Llama con `eventId = null`. Verifica excepción. |
| 15 | `registerAnonymousSpectator_throwsException_whenCategoryIdIsNull` | Guard Clause: categoryId no puede ser null. | Llama con `categoryId = null`. Verifica excepción. |
| 16 | `registerAnonymousSpectator_throwsException_whenEmailIsBlank` | Guard Clause: el email no puede estar vacío. | Llama con `email = ""`. Verifica excepción. |
| 17 | `registerAnonymousSpectator_throwsException_whenCategoryDoesNotBelongToEvent` | Que no se puede registrar en una categoría que no pertenece al evento. | Simula categoría de otro evento. Verifica excepción. |

---

## 7. EvaluacionServiceTest — 12 tests

**Tipo:** Unitario | **Clase bajo prueba:** `EvaluacionService`
**Dependencias simuladas:** `EvaluacionRepository`, `UserRepository`, `CategoryRepository`, `EvaluacionCreator` (Factory)

Las evaluaciones son las valoraciones multicriterio que hacen los expertos (ADR-006). Soportan 6 tipos: Numérica, Checklist, Rúbrica, Comentario, Audio y Vídeo. El servicio usa el **Factory Method** para crear el tipo correcto.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsAll` | Que `findAll()` devuelve todas las evaluaciones. | Simula repositorio con 2 evaluaciones. Verifica tamaño. |
| 2 | `findById_returnsExisting` | Que `findById()` devuelve la evaluación correcta. | Simula `findById(1L)`. Verifica id en el resultado. |
| 3 | `findById_throwsWhenNotFound` | Que `findById()` lanza excepción si no existe. | Simula `Optional.empty()`. Verifica excepción. |
| 4 | `create_numerica_usesFactoryMethod` | Que crear una evaluación numérica invoca el factory y persiste con el tipo correcto. | Simula `EvaluacionCreator` devolviendo `EvaluacionNumerica`. Verifica tipo `NUMERICA` en el resultado. |
| 5 | `create_checklist_usesFactoryMethod` | Que crear una evaluación checklist invoca el factory y persiste con el tipo correcto. | Igual que el anterior pero para tipo `CHECKLIST`. |
| 6 | `create_comentario_scoreIsNull` | Que las evaluaciones de tipo COMENTARIO tienen score `null` (no son numéricas). | Verifica que el campo `score` es null en el DTO devuelto. |
| 7 | `create_invalidType_throws` | Que pasar un tipo no reconocido lanza excepción. | Pasa un tipo inventado. Verifica excepción con mensaje informativo. |
| 8 | `create_negativePeso_throws` | Que el peso de la evaluación no puede ser negativo. | Pasa `peso = -1`. Verifica excepción. |
| 9 | `create_evaluadorNotFound_throws` | Que si el evaluador no existe se lanza excepción (Guard Clause). | Simula `userRepository.findById` devolviendo vacío. Verifica excepción. |
| 10 | `delete_existingEvaluacion` | Que `delete()` elimina correctamente una evaluación existente. | Verifica que `deleteById` es llamado una vez. |
| 11 | `delete_nonExisting_throws` | Que intentar borrar una evaluación que no existe lanza excepción. | Simula `findById` vacío antes del delete. Verifica excepción. |
| 12 | `findByCategory_returnsFiltered` | Que `findByCategoryId()` devuelve solo las evaluaciones de la categoría indicada. | Simula 2 evaluaciones con distintas categorías. Verifica que solo se devuelven las de la categoría correcta. |

---

## 8. UserServiceTest — 15 tests

**Tipo:** Unitario | **Clase bajo prueba:** `UserService`
**Dependencias simuladas:** `UserRepository` (Mockito)
**Patrón aplicado:** Guard Clause — todas las validaciones de id nulo o email duplicado son la primera instrucción del método (ADR-014)

Los usuarios son la entidad base del sistema. Esta clase prueba registro, login, CRUD básico y que todos los Guard Clauses funcionan correctamente.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `register_createsAndReturnsDto_whenEmailIsNew` | Que `register()` crea el usuario y devuelve el DTO cuando el email no existe. | Simula `findByEmail` devolviendo vacío y `save` con id generado. Verifica nombre en el resultado. |
| 2 | `register_throwsException_whenEmailAlreadyExists` | Guard Clause: no se puede registrar un email ya existente. | Simula `findByEmail` devolviendo un usuario. Verifica excepción con "already exists". |
| 3 | `login_returnsUserDto_whenCredentialsAreCorrect` | Que `login()` devuelve el DTO si email y contraseña son correctos. | Simula usuario con contraseña `"pass"`. Llama a `login("pass")`. Verifica resultado. |
| 4 | `login_throwsException_whenUserNotFound` | Guard Clause: si el email no existe, lanza excepción. | Simula `findByEmail` vacío. Verifica excepción. |
| 5 | `login_throwsException_whenPasswordIsWrong` | Guard Clause: si la contraseña no coincide, lanza excepción. | Simula usuario con `"pass"` pero llama con `"wrong"`. Verifica excepción. |
| 6 | `findAll_returnsAllUsersAsDto` | Que `findAll()` devuelve todos los usuarios como DTOs. | Simula 2 usuarios. Verifica tamaño y nombres. |
| 7 | `findAll_returnsEmpty_whenNoUsers` | Que `findAll()` devuelve vacío si no hay usuarios. | Simula lista vacía. Verifica resultado vacío. |
| 8 | `findById_returnsDto_whenFound` | Que `findById()` devuelve el DTO correcto cuando el usuario existe. | Simula `findById(1L)` y verifica id y nombre. |
| 9 | `findById_throwsException_whenNotFound` | Guard Clause: si el usuario no existe, lanza excepción. | Simula `Optional.empty()`. Verifica excepción. |
| 10 | `findById_throwsException_whenIdIsNull` | Guard Clause: el id no puede ser null. | Llama con `null`. Verifica excepción antes de llegar al repositorio. |
| 11 | `update_changesNameAndEmailAndReturnsUpdatedDto` | Que `update()` aplica el nuevo nombre y email y devuelve el DTO actualizado. | Simula `findById` y `save`. Verifica nuevo nombre en el resultado. |
| 12 | `update_throwsException_whenUserNotFound` | Guard Clause: si el usuario a actualizar no existe, lanza excepción. | Simula `Optional.empty()`. Verifica excepción. |
| 13 | `update_throwsException_whenIdIsNull` | Guard Clause: el id de update no puede ser null. | Llama con `id = null`. Verifica excepción. |
| 14 | `delete_callsDeleteById` | Que `delete()` llama a `deleteById` con el id correcto. | Verifica con `verify` que `deleteById(1L)` fue llamado una vez. |
| 15 | `delete_throwsException_whenIdIsNull` | Guard Clause: el id de delete no puede ser null. | Llama con `null`. Verifica excepción antes del repositorio. |

---

## 9. HojaRutaMejoraServiceTest — 13 tests

**Tipo:** Unitario | **Clase bajo prueba:** `HojaRutaMejoraService`
**Dependencias simuladas:** `HojaRutaMejoraRepository`, `UserRepository`, `CategoryRepository`, `EvaluacionRepository`, `CommentRepository`, `EventJuryRepository`, `ProjectRepository`, `VotingRepository`

La hoja de ruta de mejora sintetiza el feedback recibido por un competidor. Incluye comentarios de expertos agrupados por criterio y comentarios adicionales de la comunidad clasificados como positivo/mejora por el backend (Opción C — ADR-012).

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `getOrGenerar_cuandoExiste_devuelveExistente` | Que si ya existe una hoja guardada, se devuelve sin regenerar ni llamar a `save`. | Simula `findByCompetitorIdAndCategoryId` devolviendo la hoja existente. Verifica `never().save(any())`. |
| 2 | `getOrGenerar_cuandoNoExiste_genera` | Que si no existe hoja, se genera y persiste una nueva. | Simula `Optional.empty()` en la búsqueda. Verifica que `save` fue llamado una vez. |
| 3 | `getOrGenerar_sinCategoria_buscaPorCategoriaNula` | Que con `categoryId = null` busca por categoría nula (hoja global). | Simula `findByCompetitorIdAndCategoryIsNull`. Verifica que el DTO devuelto tiene `categoryId = null`. |
| 4 | `generar_sinComentarios_creaHojaConMensajeVacio` | Que sin evaluaciones ni comentarios, la hoja se crea con mensaje vacío y `areasMejora` vacías. | Simula evaluaciones vacías. Verifica `areasMejora.isEmpty()` y `generadoIa = false`. |
| 5 | `generar_conComentarios_agrupaPorCriterio` | Que las `EvaluacionComentario` se agrupan por criterio en `areasMejora`. | Simula 2 evaluaciones del mismo criterio. Verifica 1 área con 2 comentarios y el texto correcto. |
| 6 | `generar_ignoraEvaluacionesNoComentario` | Que las evaluaciones de tipo Numérica, Checklist, etc. son filtradas y no aparecen en `areasMejora`. | Simula solo `EvaluacionNumerica`. Verifica que `areasMejora` está vacío. |
| 7 | `generar_competidorNoExiste_lanzaExcepcion` | Guard Clause: si el competidor no existe, lanza excepción antes de generar. | Simula `userRepository.findById(99L)` vacío. Verifica excepción con "Competitor not found". |
| 8 | `generar_categoriaNoExiste_lanzaExcepcion` | Guard Clause: si la categoría no existe, lanza excepción. | Simula `categoryRepository.findById(99L)` vacío. Verifica excepción con "Category not found". |
| 9 | `generar_borraHojaPreviaAntesDeGuardar` | Que al regenerar se llama al `@Modifying` JPQL delete antes del `save`. | Verifica que `deleteByCompetitorIdAndCategoryId(1L, 5L)` y `save` son llamados en ese orden. |
| 10 | `generar_sinCategoryId_noConsultaCategoria` | Que con `categoryId = null`, `categoryRepository` nunca es consultado. | Verifica `never().findById(any())` sobre `categoryRepository`. También verifica que se usa `deleteByCompetitorIdAndCategoryIsNull`. |
| 11 | `clasificar_comentarioConKeywordMejora_esClasificadoComoMejora` | **Opción C:** Que un comentario con keyword de mejora ("pero falta") tiene `esMejora = true`. | Mockea un proyecto y un `Comment` con texto negativo. Verifica `result.getComentariosAdicionales().get(0).esMejora() == true`. |
| 12 | `clasificar_comentarioPositivo_esClasificadoComoNoMejora` | **Opción C:** Que un comentario positivo (sin keywords) tiene `esMejora = false`. | Mockea un `Comment` con texto positivo. Verifica `esMejora() == false`. |
| 13 | `clasificar_comentariosAdicionales_nuncaEsNullEnDto` | **Opción C:** Que el campo `comentariosAdicionales` nunca es `null` en el DTO, sino lista vacía. | Sin proyectos (lista vacía por defecto). Verifica `.isNotNull().isEmpty()`. |

---

## 10. EvaluacionCreatorTest — 17 tests

**Tipo:** Unitario | **Clase bajo prueba:** `EvaluacionCreator` (Factory Method — ADR-006)
**Sin dependencias externas** — prueba pura de la lógica del factory

El `EvaluacionCreator` implementa el patrón **Factory Method**: dado un tipo (`NUMERICA`, `CHECKLIST`, `RUBRICA`, `COMENTARIO`, `AUDIO`, `VIDEO`) devuelve la subclase correcta. También valida el campo `peso` y delega el cálculo de `score` en `calcularScore()`.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `numericaCreator_createsCorrectType` | Que el factory devuelve `EvaluacionNumerica` para tipo `NUMERICA`. | Llama al factory con `NUMERICA`. Verifica `instanceof EvaluacionNumerica`. |
| 2 | `checklistCreator_createsCorrectType` | Que devuelve `EvaluacionChecklist` para tipo `CHECKLIST`. | Igual con `CHECKLIST`. |
| 3 | `rubricaCreator_createsCorrectType` | Que devuelve `EvaluacionRubrica` para tipo `RUBRICA`. | Igual con `RUBRICA`. |
| 4 | `comentarioCreator_createsCorrectType` | Que devuelve `EvaluacionComentario` para tipo `COMENTARIO`. | Igual con `COMENTARIO`. |
| 5 | `audioCreator_createsCorrectType` | Que devuelve `EvaluacionAudio` para tipo `AUDIO`. | Igual con `AUDIO`. |
| 6 | `videoCreator_createsCorrectType` | Que devuelve `EvaluacionVideo` para tipo `VIDEO`. | Igual con `VIDEO`. |
| 7 | `createAndValidate_rejectNegativePeso` | Que el factory rechaza un `peso` negativo. | Pasa `peso = -0.5`. Verifica excepción. |
| 8 | `createAndValidate_acceptsZeroPeso` | Que `peso = 0` es válido (evaluación sin peso). | Pasa `peso = 0`. Verifica que no lanza excepción. |
| 9 | `createAndValidate_acceptsPositivePeso` | Que `peso > 0` es válido. | Pasa `peso = 0.3`. Verifica que no lanza excepción. |
| 10 | `createAndValidate_acceptsNullPeso` | Que `peso = null` es válido (campo opcional). | Pasa `null`. Verifica que no lanza excepción. |
| 11 | `numerica_calcularScore_sumsValues` | Que `EvaluacionNumerica.calcularScore()` suma los valores numéricos del JSON. | Crea evaluación con `{"valores": [8, 6]}`. Verifica que el score es 14. |
| 12 | `checklist_calcularScore_percentageChecked` | Que `EvaluacionChecklist.calcularScore()` devuelve el porcentaje de ítems marcados. | Crea con 3 de 4 marcados. Verifica score = 75. |
| 13 | `rubrica_calcularScore_weightedAverage` | Que `EvaluacionRubrica.calcularScore()` calcula la media ponderada según niveles. | Crea con pesos. Verifica score resultante. |
| 14 | `comentario_calcularScore_returnsNull` | Que `EvaluacionComentario.calcularScore()` devuelve `null` (no es numérica). | Verifica que el score es `null`. |
| 15 | `audio_calcularScore_withManualScore` | Que `EvaluacionAudio.calcularScore()` devuelve el score manual si está definido. | Simula score manual en los datos. Verifica el valor. |
| 16 | `audio_calcularScore_withoutManualScore` | Que sin score manual, `EvaluacionAudio.calcularScore()` devuelve `null`. | Sin score en datos. Verifica `null`. |
| 17 | `video_calcularScore_withManualScore` | Que `EvaluacionVideo.calcularScore()` devuelve el score manual si está definido. | Igual que audio pero para vídeo. |

---

## 11. CategoryRepositoryTest — 9 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `CategoryRepository`
**Base de datos:** H2 en memoria

Setup: 3 categorías precargadas — `cat1` y `cat2` en `event1`, `cat3` en `event2`.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findByEventId_returnsOnlyCategoriesOfGivenEvent` | Que `findByEventId(event1.id)` devuelve exactamente las 2 categorías de `event1`. | Ejecuta la query y verifica tamaño y nombres. |
| 2 | `findByEventId_doesNotIncludeCategoriesFromOtherEvents` | Que no incluye categorías de otros eventos. | Verifica que `cat3` no aparece en el resultado de `event1`. |
| 3 | `findByEventId_returnsEmpty_forEventWithNoCategories` | Que devuelve vacío para un evento sin categorías. | Persiste evento nuevo y verifica que su lista es vacía. |
| 4 | `findById_returnsCorrectCategory` | Que `findById` devuelve la categoría correcta con su `VotingType`. | Busca `cat1` por su id. Verifica nombre y tipo `JURY_EXPERT`. |
| 5 | `findById_returnsEmpty_whenNotFound` | Que `findById` devuelve vacío para un id inexistente. | Busca id `9999L`. Verifica Optional vacío. |
| 6 | `save_persistsNewCategory` | Que `save` genera un id y persiste la nueva categoría. | Guarda y recarga con `findById`. Verifica id no nulo. |
| 7 | `save_updatesExistingCategory` | Que `save` sobre una categoría existente actualiza sus datos. | Cambia `VotingType` a `POPULAR_VOTE`, guarda y recarga. Verifica nuevo tipo. |
| 8 | `deleteById_removesCategory` | Que `deleteById` elimina la categoría de la BD. | Elimina `cat1`, hace flush/clear y verifica que `findById` devuelve vacío. |
| 9 | `findAll_returnsAllPersistedCategories` | Que `findAll` devuelve al menos las 3 categorías insertadas. | Verifica que hay 3 o más resultados. |

---

## 12. VotingRepositoryTest — 10 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `VotingRepository`
**Base de datos:** H2 en memoria

Setup: 2 votos del mismo jurado sobre el mismo competidor con criterios distintos (25 pts y 18 pts).

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findAll_returnsAllVotings` | Que `findAll` devuelve al menos los 2 votos del setup. | Verifica tamaño ≥ 2. |
| 2 | `findById_returnsVotingWithRelations` | Que `findById` recupera el voto con todas sus relaciones (voter, project, criterion) resueltas. | Busca `voting1` y verifica los 4 campos: score, voterId, projectId, criterionId. |
| 3 | `findById_returnsEmpty_whenNotFound` | Que `findById` devuelve vacío para un id inexistente. | Busca id `99999L`. Verifica Optional vacío. |
| 4 | `save_persistsNewVoting` | Que `save` genera id y persiste un nuevo voto recuperable. | Guarda voto nuevo, limpia caché con `em.clear()` y lo recarga. Verifica score=30. |
| 5 | `save_updatesScore_whenEdited` | Que modificar el score y hacer `save` actualiza el dato en BD (intervención manual). | Cambia `voting1.score` a 40, guarda, hace flush/clear y verifica. |
| 6 | `deleteById_removesVoting` | Que `deleteById` elimina el voto de la BD. | Elimina `voting1`, hace flush/clear y verifica que `findById` devuelve vacío. |
| 7 | `deleteById_doesNotAffectOtherVotings` | Que eliminar un voto no afecta a otros votos del mismo jurado. | Elimina `voting1` y verifica que `voting2` sigue presente. |
| 8 | `count_returnsCorrectTotal` | Que `count()` cuenta correctamente el total de votos. | Verifica que el resultado ≥ 2. |
| 9–10 | *(tests adicionales Sprint 2/3)* | Queries de `findByProjectIdAndComentarioIsNotNull` y otros métodos. | Ver código fuente `VotingRepositoryTest.java`. |

---

## 13. EventParticipationRepositoryTest — 8 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `EventParticipationRepository`
**Base de datos:** H2 en memoria

Setup: 1 evento con 2 categorías y 3 participaciones — `userComp` en `catJury` (COMPETITOR), `userVoter` en `catJury` (VOTER), y `userComp` en `catPopular` (COMPETITOR).

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findByEventId_returnsAllParticipationsOfEvent` | Que `findByEventId` devuelve las 3 participaciones del evento. | Verifica tamaño = 3. |
| 2 | `findByEventIdAndCategoryId_filtersCorrectly` | Que filtra correctamente por categoría. | Filtra por `catJury`. Verifica tamaño = 2. |
| 3 | `findByEventIdAndCategoryIdAndRole_returnsOnlyCompetitors` | Que filtra por rol `COMPETITOR` en `catJury` y devuelve solo `userComp`. | Verifica tamaño = 1 y que el usuario es `userComp`. |
| 4 | `findByEventIdAndCategoryIdAndRole_returnsOnlyVoters` | Que filtra por rol `VOTER` en `catJury` devuelve solo `userVoter`. | Verifica tamaño = 1 y que el usuario es `userVoter`. |
| 5 | `exists_returnsTrue_whenAlreadyRegistered` | Que `existsByEventIdAndUserIdAndCategoryId` devuelve `true` para combinación existente. | Comprueba `userComp` en `catJury`. Verifica `true`. |
| 6 | `exists_returnsFalse_whenNotRegistered` | Que devuelve `false` para combinación inexistente. | Comprueba `userVoter` en `catPopular` (donde no está inscrito). Verifica `false`. |
| 7 | `findByEventAndUserAndCategory_returnsCorrectParticipation` | Que la búsqueda combinada devuelve exactamente la participación correcta. | Busca `userComp` en `catJury`. Verifica rol `COMPETITOR`. |
| 8 | `findByUserId_returnsAllParticipationsOfUser` | Que `findByUserId` devuelve todas las participaciones de un usuario en todos sus eventos/categorías. | Busca participaciones de `userComp` (en 2 categorías). Verifica tamaño = 2. |

---

## 14. EventRepositoryTest — 5 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `EventRepository`
**Base de datos:** H2 en memoria | **ADR:** ADR-013

Prueba la query derivada `existsByOrganizerId`, que navega la relación `@ManyToOne` hacia `User` y es usada en el servicio como Guard Clause.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `existsByOrganizerId_returnsTrue_whenUserIsOrganizer` | Que devuelve `true` cuando el usuario es organizador del evento. | Persiste evento con organizador. Verifica `true`. |
| 2 | `existsByOrganizerId_returnsFalse_whenUserIsNotOrganizer` | Que devuelve `false` cuando el usuario existe pero no es organizador de ese evento. | Persiste usuario no organizador. Verifica `false`. |
| 3 | `existsByOrganizerId_returnsFalse_whenUserIdDoesNotExist` | Que devuelve `false` para un id de usuario que no existe en la BD. | Consulta con id `9999L`. Verifica `false`. |
| 4 | `existsByOrganizerId_returnsTrue_whenUserOrganizesMultipleEvents` | Que funciona correctamente cuando el mismo usuario organiza varios eventos. | Persiste 2 eventos con el mismo organizador. Verifica `true` para ambos. |
| 5 | `existsByOrganizerId_returnsFalse_whenEventHasNoOrganizer` | Que devuelve `false` si el evento no tiene organizador asignado. | Persiste evento con `organizer = null`. Verifica `false`. |

---

## 15. ProjectRepositoryTest — 6 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `ProjectRepository`
**Base de datos:** H2 en memoria | **ADR:** ADR-013

Prueba la query JPQL personalizada `findByCompetitorId` que hace un JOIN sobre la relación `@ManyToMany` entre `Project` y `Competitor`. Es la query usada en `HojaRutaMejoraService.recogerComentariosAdicionales()`.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `findByCompetitorId_returnsProjectsWhereUserIsCompetitor` | Que la query devuelve los proyectos en los que el usuario es competidor. | Persiste competidor con proyecto. Verifica que el proyecto aparece en el resultado. |
| 2 | `findByCompetitorId_returnsEmpty_whenUserHasNoProjects` | Que devuelve lista vacía si el usuario no tiene proyectos. | Persiste usuario sin proyectos. Verifica resultado vacío. |
| 3 | `findByCompetitorId_returnsEmpty_whenUserIdDoesNotExist` | Que devuelve lista vacía para un id de usuario inexistente. | Consulta con id `9999L`. Verifica resultado vacío. |
| 4 | `findByCompetitorId_doesNotReturnProjectsOfOtherCompetitors` | Que el resultado no contiene proyectos de otros competidores. | Persiste 2 competidores con proyectos distintos. Verifica aislamiento. |
| 5 | `findByCompetitorId_returnsSingleProject_whenUserBelongsToOne` | Caso base: usuario con un único proyecto. | Persiste 1 competidor con 1 proyecto. Verifica tamaño = 1. |
| 6 | `findByCompetitorId_isolatesResultsByCompetitor` | Que dos competidores distintos tienen resultados completamente aislados. | Persiste A con proyectoX y B con proyectoY. Verifica que A no ve proyectoY y viceversa. |

---

## 16. EvaluacionRepositoryTest — 6 tests

**Tipo:** Integración (`@DataJpaTest`) | **Clase bajo prueba:** `EvaluacionRepository`
**Base de datos:** H2 en memoria

Prueba la persistencia de las distintas subclases de evaluación y las queries filtradas por categoría y competidor.

| Nº | Nombre del test | Qué evalúa | Cómo lo hace |
|----|-----------------|------------|--------------|
| 1 | `save_numerica_persistsCorrectly` | Que `EvaluacionNumerica` se persiste y recupera con su tipo correcto. | Guarda y recarga. Verifica `instanceof EvaluacionNumerica`. |
| 2 | `save_checklist_persistsCorrectly` | Que `EvaluacionChecklist` se persiste correctamente. | Igual con `EvaluacionChecklist`. |
| 3 | `save_comentario_persistsCorrectly` | Que `EvaluacionComentario` se persiste correctamente. | Igual con `EvaluacionComentario`. |
| 4 | `findByCategoryId_returnsFiltered` | Que `findByCategoryId` devuelve solo las evaluaciones de esa categoría. | Persiste 2 evaluaciones en categorías distintas. Verifica que solo se devuelve la correcta. |
| 5 | `findByCompetitorId_returnsFiltered` | Que `findByCompetitorId` devuelve solo las evaluaciones de ese competidor. | Persiste 2 evaluaciones de competidores distintos. Verifica aislamiento. |
| 6 | `findByCategoryAndCompetitor_returnsFiltered` | Que la query combinada por categoría Y competidor filtra correctamente. | Persiste combinaciones cruzadas. Verifica que solo se devuelve la intersección correcta. |

---

## Resumen por clase

| Clase | Tipo | Tests | Cubre |
|-------|------|-------|-------|
| `CriterionServiceTest` | Unitario | 14 | CRUD de criterios de evaluación |
| `EventServiceTest` | Unitario | 9 | CRUD de eventos, creación con organizador |
| `CategoryServiceTest` | Unitario | 25 | CRUD, tipos de voto, puntos, límites voto popular, fechas |
| `VotingServiceTest` | Unitario | 28 | Votos, validaciones periodo, límites popular, auto-voto, duplicados, intervención manual |
| `ProjectServiceTest` | Unitario | 9 | CRUD de proyectos, comentarios, asociación de competidores |
| `EventParticipationServiceTest` | Unitario | 17 | Registro participantes, duplicados, espectadores anónimos |
| `EvaluacionServiceTest` | Unitario | 12 | Evaluaciones multicriterio, Factory Method, validaciones |
| `UserServiceTest` | Unitario | 15 | CRUD usuarios, Guard Clause en todas las entradas (ADR-014) |
| `HojaRutaMejoraServiceTest` | Unitario | 13 | Hoja de ruta, agrupación por criterio, clasificación positivo/mejora (Opción C) |
| `EvaluacionCreatorTest` | Factory | 17 | Factory Method 6 tipos, validación peso, calcularScore() por tipo |
| `CategoryRepositoryTest` | Integración | 9 | Queries JPA sobre categorías en BD real |
| `VotingRepositoryTest` | Integración | 10 | Persistencia votos, relaciones, intervención manual en BD |
| `EventParticipationRepositoryTest` | Integración | 8 | Queries complejas de participaciones por evento, categoría y rol |
| `EventRepositoryTest` | Integración | 5 | `existsByOrganizerId` — query derivada @ManyToOne (ADR-013) |
| `ProjectRepositoryTest` | Integración | 6 | `findByCompetitorId` — JPQL JOIN @ManyToMany (ADR-013) |
| `EvaluacionRepositoryTest` | Integración | 6 | Persistencia subclases evaluación, queries por categoría/competidor |
| **Total** | | **203** | |
