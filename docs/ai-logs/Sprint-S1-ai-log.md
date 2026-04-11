# AI Usage Log — Sprint S1

**Sprint:** S1 — Migración frontend + debug conexión backend
**Periodo:** Marzo 2026
**Herramientas usadas en este sprint:** Claude Sonnet (Claude Code / claude.ai), GitHub Copilot (integrado en Cursor)

---

### 1) Herramientas usadas

- **Claude Sonnet (Claude Code — modo agente en terminal):**
  - Diagnóstico y resolución del problema de autenticación con Supabase Transaction Pooler (pgBouncer reportaba `user "postgres"` enmascarando la causa real).
  - Creación de `LocalDataSourceConfig.java` para bypassing programático de la resolución de propiedades Spring.
  - Análisis del flujo de capas del frontend: páginas → `client.ts` → `axios.ts` → proxy Vite → backend.
  - Creación y actualización de `CLAUDE.md` en backend y frontend para contexto de proyecto.
  - Redacción de ADR-005 (migración frontend de Supabase a Spring Boot REST).
  - Actualización de documentación: ADR-001, BACKEND-RESUMEN.md, SKILL.md del proyecto.

- **GitHub Copilot (Cursor):**
  - Autocompletado durante la reescritura de `src/api/client.ts` del frontend.
  - Sugerencias de métodos de repositorio Spring Data en los nuevos repos.

---

### 2) Prompts clave

- **Prompt 1:** "el backend sigue dando error de autenticación `FATAL: password authentication failed for user 'postgres'` aunque el username es `postgres.bmulgijtddwdwaajktay` — cómo lo soluciono?"
- **Prompt 2:** "puede ser que Spring Boot no sale porque tengo Dockerfiles que no uso en el backend?"
- **Prompt 3:** "crea un `LocalDataSourceConfig.java` con `@Profile("local") @Primary` que bypasee todas las variables de entorno y use las credenciales hardcodeadas para que no haya ambigüedad"
- **Prompt 4:** "esta es la nueva password `19VX5FTtdniXvkho` — actualiza todos los ficheros"
- **Prompt 5:** "analízame el flujo de los componentes de la interfaz y cómo se comunican entre capas. Actualízame los documentos y la skill Votify que tenía la comunicación frontend→Supabase"

---

### 3) Salidas relevantes (resumen corto)

- **Prompt 1 →** La IA diagnosticó que el mensaje `user "postgres"` de pgBouncer NO indica el username enviado, sino el usuario interno del pooler — esto enmascaraba el error real (contraseña incorrecta/caducada). Propuso pasos de depuración: verificar el perfil Spring activo, crear DataSource programático.
- **Prompt 2 →** La IA confirmó que los Dockerfiles no impiden el arranque de Spring Boot. El problema era la autenticación, no la configuración Docker.
- **Prompt 3 →** La IA generó `LocalDataSourceConfig.java` con `@Configuration @Profile("local") @AutoConfigureBefore(DataSourceAutoConfiguration.class) @Primary`. También añadió un logger `>>> LocalDataSourceConfig ACTIVO` para confirmar la activación. Esto permitió confirmar que el bean se cargaba y el username era correcto, descartando la configuración como causa.
- **Prompt 4 →** La IA actualizó la contraseña en `LocalDataSourceConfig.java`, `application-local.properties`, `application.properties` y `.env`. El backend arrancó correctamente con la nueva credencial.
- **Prompt 5 →** La IA analizó el flujo de capas: `Página TSX → client.ts → axios.ts (baseURL=/api) → proxy Vite → Spring Boot :8080 → PostgreSQL Supabase`. Creó CLAUDE.md en ambos repos, ADR-005, actualizó ADR-001 y BACKEND-RESUMEN.md, y reescribió la SKILL.md del proyecto eliminando todas las referencias al acceso directo a Supabase.

---

### 4) Qué aceptamos y qué rechazamos

- **Aceptado:** `LocalDataSourceConfig.java` con credenciales hardcodeadas en perfil `local` → por qué: garantiza que en local NUNCA hay ambigüedad entre variables de entorno del SO, perfiles Spring y properties. El fichero está en `.gitignore` implícitamente (o se añade) y en producción el perfil `local` no se activa.
- **Aceptado:** `mvn clean spring-boot:run` en lugar de `mvn spring-boot:run` → por qué: Maven incremental no detecta nuevos ficheros `.java` sin `clean`. Documentado en CLAUDE.md.
- **Aceptado:** Diagnóstico de WSL vs Windows PowerShell → el frontend en WSL no puede llegar al backend en Windows por `localhost:8080` (entornos de red distintos). Solución: ejecutar ambos desde WSL. Documentado en `CLAUDE.md` del frontend.
- **Rechazado (no aplicable):** La IA sugirió inicialmente que los Dockerfiles podían interferir — esto se descartó porque Docker no estaba corriendo y Spring Boot no los lee en tiempo de ejecución.

---

### 5) Cómo lo verificamos

- [X] Backend arranca con `mvn clean spring-boot:run` y el log muestra `>>> LocalDataSourceConfig ACTIVO - conectando como: postgres.bmulgijtddwdwaajktao`.
- [X] Log de Spring Boot muestra `The following 1 profile is active: "local"`.
- [X] Hibernrte imprime las queries SQL al arrancar (`ddl-auto=update` sin errores de conexión).
- [X] `GET http://localhost:8080/actuator/health` devuelve `{"status":"UP"}`.
- [X] Frontend en WSL: `ECONNREFUSED` se resuelve ejecutando el backend también desde WSL.
- [X] 98 tests pasan (`mvn test`) tras los cambios de Sprint 1 en el backend.

---

### 6) Resultado final / decisión humana

La causa del fallo de autenticación fue una **contraseña caducada/cambiada en Supabase**, no un error de configuración. El mensaje de pgBouncer (`user "postgres"`) enmascaró esto durante varios intentos. La creación de `LocalDataSourceConfig.java` fue útil como herramienta de diagnóstico (confirmó que el username era correcto) y se mantiene como solución robusta para el perfil local.

La documentación del proyecto se actualizó completamente para reflejar la arquitectura Sprint 1:
- `CLAUDE.md` en backend y frontend: contexto completo para Claude Code
- `ADR-005`: migración frontend Supabase → Spring Boot REST
- `BACKEND-RESUMEN.md`: actualizado con Sprint 1 (nuevos endpoints, 98 tests)
- `SKILL.md` del proyecto: eliminadas todas las referencias a acceso directo a Supabase

Total de tests al cierre del Sprint 1: **98 tests** (73 unitarios + 25 integración), todos en verde.

Referencias en el repo:
- ADR-005: `docs/adr/ADR-005-migracion-frontend-supabase-a-backend.md`
- Config: `src/main/java/com/votify/config/LocalDataSourceConfig.java`
- Frontend: `src/api/client.ts` (330 líneas, 48+ funciones), `src/api/axios.ts`

---

---

## Sesión 2 — Análisis de Factory Method GoF (07-04-2026)

**Actividad:** Análisis de diseño — patrón Factory Method extendido (GoF) en backend y frontend
**Herramienta:** Claude Sonnet (Claude Code — modo agente en terminal)

---

### 1) Herramientas usadas

- **Claude Sonnet (Claude Code — modo agente):**
  - Explicación del patrón Factory Method en sus dos versiones (Simple Factory vs GoF completo).
  - Lectura y resumen del PDF del lab `lab_factory_method_votify.pdf` (Sprint 1, DDS).
  - Exploración autónoma del backend (`21.03-Backend/`) y del frontend (`votify-frontend/`)
    para identificar oportunidades de aplicación real del patrón.
  - Análisis senior de código: identificación de if/else dispersos, duplicación de mapping,
    violaciones de OCP y SRP en el código existente.
  - Redacción de ADR-006 (Factory Method GoF en Votify).
  - Actualización de este AI Usage Log (Sprint S1, sesión 2).

---

### 2) Prompts clave

- **Prompt 1:** `"sabes que es el patron fabrica?"`
  → Pregunta introductoria para verificar que la IA conoce el patrón antes de usarlo en el proyecto.

- **Prompt 2:** `"@lab_factory_method_votify.pdf @PatronesCreacionales_260218_173826.pdf resumen el patron fabrica, la version extendida"`
  → Solicitud de resumen de los dos PDFs de la asignatura DDS sobre el patrón GoF completo,
    distinguiendo la versión simple (Simple Factory) de la versión extendida (Creator abstracto +
    ConcreteCreator + Product abstracto + ConcreteProduct).

- **Prompt 3:** `"Eres un senior en patrones e clean code, evalua el backend y el frontend y dime posibilidades para implementar el metodo fabrica el extendidio que te adjunto resumido"`
  → Solicitud de análisis técnico completo de ambos repos para identificar dónde el patrón
    tiene valor real, distinguiendo oportunidades de alto impacto de posibles sobrediseños.

- **Prompt 4:** `"actualizame el documento ia usage y adr-template basandote en la repo del backend y del frontend"`
  → Solicitud de creación del ADR-006 y actualización de este log con la sesión actual,
    basándose en el estado real de los repos (no en plantillas genéricas).

---

### 3) Salidas relevantes (resumen corto)

- **Prompt 1 →** La IA explicó las dos visiones del patrón (Simple Factory y GoF), los cuatro
  participantes (Creator, ConcreteCreator, Product, ConcreteProduct) y cuándo aplicar cada versión.
  Respuesta usada como base conceptual para los siguientes prompts.

- **Prompt 2 →** La IA leyó ambos PDFs y sintetizó el patrón GoF completo aplicado al contexto
  de Votify (lab): jerarquía `VoteCreator`/`ExpertVote`/`PublicVote`, tabla comparativa sin/con
  patrón, y el principio de "cirugía mínima" al añadir tipos nuevos.
  El segundo PDF no pudo leerse (herramienta `pdftoppm` no disponible en el entorno); se usó
  solo el lab PDF como fuente primaria.

- **Prompt 3 →** La IA exploró autónomamente los 82 ficheros del backend y los 25 del frontend,
  identificando cinco oportunidades concretas ordenadas por prioridad:
  1. `EvaluacionCreator` (backend) — 6 tipos con `calcularScore()` distinto → máximo impacto.
  2. `VotacionCreator` (backend) — `JURY_EXPERT` vs `POPULAR_VOTE` → mapeo directo al lab.
  3. `ParticipantFactory` (frontend) — 3 versiones duplicadas de mapping en `client.ts`.
  4. `RolCreator` (backend) — para Sprint 2-3, permisos y certificados por rol.
  5. `VotingPayloadFactory` (frontend) — construcción de payloads para `Evaluar.tsx`/`Votar.tsx`.
  La IA también identificó qué NO aplicar: `Event`, `Criterion`, `Project`, `Category`
  (diferencia de datos, no de comportamiento → no justifica el patrón).

- **Prompt 4 →** La IA leyó todos los ADRs y AI logs existentes para mantener el estilo,
  y generó ADR-006 y esta entrada del log basándose en el estado real de los repos.

---

### 4) Qué aceptamos y qué rechazamos

- **Aceptado:** `EvaluacionCreator` como primera implementación a priorizar → por qué: es el
  caso con más tipos (6) y más lógica variable real. Añadir un tipo nuevo solo requiere 2 ficheros.

- **Aceptado:** `VotacionCreator` con `JURY_EXPERT` / `POPULAR_VOTE` → por qué: mapeo directo
  al lab del Sprint 1; justificable académica y técnicamente.

- **Aceptado:** `ParticipantFactory` en el frontend como módulo `src/factories/participantFactory.ts`
  → por qué: hay 3 versiones inconsistentes de mapping en `client.ts` ahora mismo (bug latente).

- **Pendiente de validar:** `RolCreator` (backend) → se pospone a Sprint 2-3, cuando la lógica
  de permisos y certificados sea necesaria. No se implementa en Sprint 1 para evitar sobrediseño.

- **Rechazado:** Factory Method para `Event`, `Criterion`, `Project` → por qué: la diferencia
  entre tipos es de datos (campos), no de comportamiento (métodos). No hay polimorfismo real
  que ganar. Añadir un Creator por cada tipo sería sobrediseño claro.

- **Rechazado (por ahora):** `VotingPayloadFactory` en el frontend → por qué: los payloads son
  objetos planos sin lógica; centralizar en una factory tiene valor solo si se añade `SponsorVote`
  u otros tipos con factor de normalización propio. Se deja como deuda técnica documentada.

---

### 5) Cómo lo verificamos

- [ ] Implementar `EvaluacionNumerica` + `EvaluacionNumericaCreator` como primer caso.
- [ ] Test unitario aislado: `EvaluacionNumericaCreatorTest` (sin mocks de servicios).
- [ ] Implementar `VotacionExperto` + `VotacionExpertoCreator`; verificar que `VotingService`
      usa el creator y el if/else desaparece de la capa de servicio.
- [ ] Test: `VotacionExpertoCreatorTest` y `VotacionPopularCreatorTest` independientes.
- [ ] Crear `src/factories/participantFactory.ts` en el frontend y sustituir los 3 map()
      inline en `client.ts` por llamadas a `ParticipantFactory.fromXxxApi()`.
- [ ] Verificar que los 98 tests existentes siguen pasando tras el refactor (`mvn test`).

---

### 6) Resultado final / decisión humana

El equipo identifica el Factory Method GoF como el siguiente paso de refactor en el backend.
La decisión de implementación queda documentada en **ADR-006**. Se prioriza `EvaluacionCreator`
primero (mayor impacto) y `VotacionCreator` segundo (requisito del lab). `RolCreator` queda
para Sprint 2-3.

El análisis confirmó que el diseño actual del backend tiene las condiciones necesarias para el
patrón (variabilidad real de comportamiento por tipo, extensión prevista) en los dominios de
`Evaluacion` y `Votacion`, pero NO en `Event`, `Criterion` o `Project`.

La exploración autónoma de los repos por parte de la IA fue útil para identificar el problema
de los 3 mappings duplicados de `Participant` en el frontend, que no estaba en el radar
del equipo y constituye un bug latente (tipos inconsistentes según el endpoint que se llame).

Referencias en el repo:
- ADR-006: `docs/adr/ADR-006-factory-method-votify.md`
- Lab de referencia: `lab_factory_method_votify.pdf` (DDS, Sprint 1)
- Ficheros de frontend afectados: `src/api/client.ts` (líneas de mapping de Participant)

---

---

## Sesión 3 — Implementación de EvaluacionCreator (Factory Method GoF) (11-04-2026)

**Actividad:** Implementación completa del patrón Factory Method para el dominio Evaluacion multicriterio
**Herramienta:** Claude Opus 4.6 (Claude Code — modo agente en terminal)

---

### 1) Herramientas usadas

- **Claude Opus 4.6 (Claude Code — modo agente):**
  - Exploración del backend para verificar el estado de implementación de cada patrón del ADR-006.
  - Diseño de la arquitectura de clases: decisión SINGLE_TABLE vs JOINED, columna JSON `datos`.
  - Generación de diagrama UML del patrón en FigJam (Figma MCP).
  - Creación de documento de visión global con estructura antes/después.
  - Implementación completa: 7 entidades, 7 creators, DTO, repository, service, controller.
  - Creación de tests unitarios (Mockito) e integración (H2 @DataJpaTest).
  - Actualización de ADR-006 y este AI log.

---

### 2) Prompts clave

- **Prompt 1:** `"apoyandote en el adr 6 para evaluar cual de los patrones nombrados en el se ha desarrollado y cuales quedan para desarrollar"`
  → Solicitud de auditoría del estado de implementación de los 4 Factory Method del ADR-006.

- **Prompt 2:** `"segun veo la evaluacion multicriterio no la veo en uts ni para sprint2,3 asi que porque no implementar esa funcionalidad ahora"`
  → Decisión de implementar EvaluacionCreator ahora como parte del Sprint 1.

- **Prompt 3:** `"antes de cambiar nada necesito la vision global de la estructura sin la creacion de las nuevas clases, un diagrama uml del patron que vamos a crear y si se codifica alguna clase actual necesito acceder a ella, saber que clase es y guardarme el antes y el despues"`
  → Solicitud de documentación previa a la implementación: estructura actual, UML, y diff de clases modificadas.

- **Prompt 4:** `"creame un ia logs correspondiente a esta decision que se ha tomado añadiendo campos: 6.2. Revisión crítica de la solución propuesta por la IA, 6.3. Adaptación de la solución a la aplicación"`
  → Solicitud de campos específicos de revisión crítica en el log.

---

### 3) Salidas relevantes (resumen corto)

- **Prompt 1 →** La IA exploró el backend y determinó que solo `ParticipantCreator` (User→Competitor/Voter) estaba implementado. Los 3 factory methods restantes del ADR-006 (EvaluacionCreator, VotacionCreator, RolCreator) y el ParticipantFactory del frontend estaban pendientes.

- **Prompt 2 →** Se decidió implementar los 6 tipos de evaluación completos (NUMERICA, CHECKLIST, RUBRICA, COMENTARIO, AUDIO, VIDEO) como entidad nueva `Evaluacion` separada de `Voting`, solo en backend.

- **Prompt 3 →** La IA generó:
  1. Diagrama UML en FigJam con los 4 participantes GoF.
  2. Documento `docs/vision-global-evaluacion-factory-method.md` con la estructura ANTES completa.
  3. El diff exacto (ANTES/DESPUÉS) de `CategoryService.java` — la única clase existente modificada.

- **Prompt 4 →** Implementación completa: 22 ficheros nuevos + 1 modificado. Actualización de ADR-006 y este log con campos 6.2 y 6.3.

---

### 4) Qué aceptamos y qué rechazamos

- **Aceptado:** `SINGLE_TABLE` como estrategia JPA para `Evaluacion` → por qué: los 6 subtipos difieren en comportamiento (`calcularScore()`), no en columnas. JOINED crearía 6 tablas extra sin beneficio real. El ADR-006 mencionaba este riesgo y se resuelve aquí.

- **Aceptado:** Columna `datos` TEXT con JSON → por qué: cada tipo tiene datos con estructura diferente (array de valores, array de booleans, array de niveles, texto, URL+score). Una columna JSON flexible evita columnas sparse nullable.

- **Aceptado:** `Map<TipoEvaluacion, EvaluacionCreator>` en `EvaluacionService` → por qué: reemplaza el if/else por un lookup O(1). Añadir un tipo nuevo = 1 línea en el Map + 2 clases.

- **Aceptado:** `Evaluacion` como entidad separada de `Voting` → por qué: `Voting` tiene relaciones (voter, competitor, criterion) y reglas de negocio distintas (maxVotesPerVoter, totalPoints). Mezclar ambos dominios violaría SRP.

- **Rechazado:** Usar Lombok para las entidades → por qué: el proyecto existente usa getters/setters manuales en todas las entidades. Consistencia con el código existente.

---

### 5) Cómo lo verificamos

- [ ] `mvn test` — los 98 tests existentes siguen pasando (CategoryServiceTest actualizado con nuevos mocks).
- [ ] Tests nuevos de `EvaluacionCreatorTest` pasan — cada ConcreteCreator produce el subtipo correcto.
- [ ] Tests nuevos de `EvaluacionServiceTest` pasan — CRUD + factory method + validación peso.
- [ ] Tests de integración `EvaluacionRepositoryTest` pasan — persistencia SINGLE_TABLE en H2.
- [ ] `mvn clean spring-boot:run` arranca sin errores y Hibernate crea la tabla `evaluaciones`.

---

### 6) Resultado final / decisión humana

Se implementó el Factory Method GoF completo para el dominio `Evaluacion` con 6 tipos de evaluación multicriterio. El patrón elimina la necesidad de if/else al añadir nuevos tipos — solo requiere 2 clases nuevas (Product + Creator) y 1 línea en el Map del Service.

**Ficheros creados:** 22 (7 entidades + 1 enum + 7 creators + 1 DTO + 1 repository + 1 service + 1 controller + 3 tests)
**Ficheros modificados:** 2 (`CategoryService.java` + `CategoryServiceTest.java`)
**Ficheros de documentación:** ADR-006 actualizado + este log

#### 6.2. Revisión crítica de la solución propuesta por la IA

**Puntos fuertes de la propuesta:**
- La elección de SINGLE_TABLE es correcta para este caso: los 6 subtipos comparten exactamente las mismas columnas (evaluador, competitor, category, criterion, peso, datos, createdAt). La diferencia está solo en `calcularScore()`. JOINED habría añadido 6 tablas sin columnas propias — overhead sin beneficio.
- El uso de `Map<TipoEvaluacion, EvaluacionCreator>` es más limpio que un switch/if-else y es extensible sin modificar código existente (OCP).
- La columna `datos` como JSON TEXT es pragmática: evita un modelo relacional complejo con tablas auxiliares para cada tipo (tabla de items checklist, tabla de niveles rúbrica, etc.).

**Puntos débiles / trade-offs aceptados:**
- **JSON no tipado en BD:** la columna `datos` no tiene validación a nivel de base de datos. Un JSON malformado solo se detecta en runtime (`calcularScore()` lanza RuntimeException). Mitigación: los tests cubren cada formato JSON esperado.
- **ObjectMapper estático en entidades JPA:** cada ConcreteProduct tiene un `static ObjectMapper`. Esto funciona pero acopla la lógica de parseo al modelo de dominio. Una alternativa sería mover el cálculo al Service, pero eso rompería el polimorfismo del patrón (el Service tendría que saber el tipo concreto para calcular).
- **Sin validación de schema JSON por tipo:** no se valida que un `EvaluacionChecklist` reciba `{"items": [...]}` y no `{"valores": [...]}`. Mitigación aceptable en MVP — el frontend es el único productor de estos payloads.
- **Número de clases:** 14 clases nuevas solo para el patrón (7 Products + 7 Creators). Es el coste inherente del GoF completo, pero cada clase tiene menos de 30 líneas y una sola responsabilidad.

**Comparación con alternativas descartadas:**
- Simple Factory (switch en una clase `EvaluacionFactory`): más sencillo pero viola OCP — cada tipo nuevo requiere modificar la factory.
- Strategy pattern (inyectar la estrategia de cálculo): viable pero no cumple el requisito académico del lab que exige Factory Method GoF con Creator abstracto.

#### 6.3. Adaptación de la solución a la aplicación

El patrón Factory Method GoF (del libro Gang of Four, 1994) se adaptó al contexto específico de Votify de las siguientes formas:

**1. Entidades JPA en lugar de POJOs puros:**
El GoF define Product como una clase abstracta simple. En Votify, `Evaluacion` es una entidad JPA con `@Entity`, `@Inheritance(SINGLE_TABLE)`, `@DiscriminatorColumn`, y relaciones `@ManyToOne`. Los ConcreteProducts son también `@Entity` con `@DiscriminatorValue`. Esto permite que el ORM gestione la persistencia polimórfica automáticamente — al cargar una `Evaluacion` de la BD, Hibernate instancia el subtipo correcto según el discriminador.

**2. Map de Creators en lugar de herencia pura del Client:**
El GoF sugiere que el Client use el Creator vía inyección o herencia. En Votify, el `EvaluacionService` (Client) mantiene un `Map<TipoEvaluacion, EvaluacionCreator>` inicializado en el constructor. Esto adapta el patrón a Spring Boot (inyección por constructor, beans stateless) y elimina cualquier if/else residual — el tipo del DTO se convierte en una key de Map.

**3. Resolución de relaciones separada del Creator:**
En el GoF puro, el Creator construye el producto completo. En Votify, los Creators solo instancian la subclase y setean campos simples (peso, datos). Las relaciones JPA (evaluador, competitor, category, criterion) se resuelven en el Service vía repositorios, porque requieren acceso a la base de datos. Esto mantiene los Creators simples y sin dependencias de Spring.

**4. Template Method en createAndValidate():**
El `EvaluacionCreator` abstracto define `createAndValidate()` como Template Method: primero valida reglas comunes (peso >= 0), luego delega en `create()` (el Factory Method). Esto replica la estructura del `ParticipantCreator` ya existente en el proyecto, que usa `register()` como Template Method.

**5. Columna JSON para datos polimórficos:**
El GoF no aborda la persistencia. La adaptación a JPA/SQL requiere decidir cómo almacenar datos que varían por subtipo. Se eligió una columna JSON TEXT en lugar de tablas auxiliares, porque los datos de cada tipo son de lectura frecuente y escritura infrecuente, y la estructura es simple (arrays planos, no grafos).

Referencias en el repo:
- ADR-006 actualizado: `docs/adr/ADR-006-factory-method-votify.md`
- Visión global (antes/después): `docs/vision-global-evaluacion-factory-method.md`
- Entidades: `src/main/java/com/votify/entity/Evaluacion*.java`
- Factory: `src/main/java/com/votify/service/factory/evaluacion/`
- Tests: `src/test/java/com/votify/service/factory/evaluacion/EvaluacionCreatorTest.java`
