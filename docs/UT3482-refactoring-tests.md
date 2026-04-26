# UT 3482 — Control de Votos: Refactoring y Tests Unitarios

Sprint 2 · Votify Backend (`21.03-Backend`) + Frontend (`votify-frontend`)

---

## 3. Refactoring y Tests Unitarios (1,5 puntos)

---

### 3.1 Refactorizaciones aplicadas

#### R-01 — Introduce Guard Clause · `VotingService.java` (PA-1314)

**Nombre Fowler:** *Introduce Guard Clause* (también clasificado como caso de *Replace Nested Conditional with Guard Clauses*).

**Objetivo:** El método `create()` de `VotingService` detectaba si ya existía un voto para la combinación `(voter, competitor, criterion, category)` y, en ese caso, **actualizaba silenciosamente** el registro existente sumando o reemplazando la puntuación. Este comportamiento violaba la PA-1314, que exige bloquear el segundo voto con un mensaje de error explícito. La refactorización sustituye el bloque condicional complejo por una cláusula de guarda que lanza la excepción en el borde de entrada del método.

**Beneficios:**
- El flujo "feliz" queda libre de ramificaciones adicionales.
- El contrato del método queda explícito: si ya votaste, no se procesa nada.
- Elimina 16 líneas de lógica de actualización que mezclaban dos responsabilidades distintas (crear vs. editar).

**Código ANTES:**

```java
// VotingService.java — método create(), líneas 80-98 (antes del cambio)

java.util.Optional<Voting> existingOpt = votingRepository
        .findExistingVote(voter.getId(), competitor.getId(), criterion.getId(), categoryId);

if (existingOpt.isPresent()) {
    Voting existing = existingOpt.get();
    int add = dto.getScore() != null ? dto.getScore() : 0;
    Category existingCat = existing.getCategory();
    if (existingCat != null && existingCat.getVotingType() == VotingType.JURY_EXPERT) {
        existing.setScore(add);
    } else {
        int current = existing.getScore() != null ? existing.getScore() : 0;
        existing.setScore(current + add);
    }
    if (categoryId != null && existing.getCategory() == null) {
        existing.setCategory(category);
    }
    existing.setComentario(normalizeComment(dto.getComentario()));
    evaluateVote(existing);
    return toDto(votingRepository.save(Objects.requireNonNull(existing)));
}
```

**Código DESPUÉS:**

```java
// VotingService.java — método create() (después del cambio)

// PA-1314: Guard Clause — un votante no puede emitir dos votos para la misma
// combinación (voter, competitor, criterion, category). Se rechaza con excepción
// explícita en lugar de actualizar silenciosamente el registro existente.
if (votingRepository.findExistingVote(
        voter.getId(), competitor.getId(), criterion.getId(), categoryId).isPresent()) {
    throw new RuntimeException("Ya has votado este proyecto.");
}
```

---

#### R-02 — Extract Function · `Votar.tsx` (PA-1317)

**Nombre Fowler:** *Extract Function* (aplicado a un efecto reactivo independiente).

**Objetivo:** La evaluación de `isPeriodActive(currentCat)` se calculaba **una sola vez** al montar el componente o al cambiar la categoría. Si un supervisor cerraba el periodo mientras el votante tenía la pantalla abierta, la UI no se actualizaba. La refactorización extrae la lógica de refresco de categoría en una función `refreshCurrentCategory` y la invoca periódicamente mediante `setInterval`, creando un efecto de *polling* independiente del ciclo de carga principal.

**Beneficios:**
- El mecanismo de actualización temporal queda aislado en su propio `useEffect`.
- La función `refreshCurrentCategory` es fácilmente testeable y sustituible por una suscripción Realtime en el futuro.
- No afecta al rendimiento: 30 s de intervalo implica una petición GET ligera por categoría.

**Código ANTES:**

```tsx
// Votar.tsx — no existía ningún mecanismo de refresco temporal

useEffect(() => {
  if (!selEvent || !selCat) return;
  const cat = categories.find(c => String(c.id) === selCat);
  setCurrentCat(cat || null);          // ← calculado solo al montar / cambiar selCat
  // ...resto de la carga de datos
}, [selEvent, selCat, categories]);
```

**Código DESPUÉS:**

```tsx
// Votar.tsx — useCallback + useEffect de polling añadidos

// PA-1317: refresco de categorías cada 30 s para detectar cambios de periodo en tiempo real
const refreshCurrentCategory = useCallback(() => {
  if (!selEvent || !selCat) return;
  getCategoriesByEvent(selEvent).then(cats => {
    const updated = cats.find(c => String(c.id) === selCat);
    setCurrentCat(updated || null);
  }).catch(() => {});
}, [selEvent, selCat]);

useEffect(() => {
  const interval = setInterval(refreshCurrentCategory, 30_000);
  return () => clearInterval(interval);   // limpieza al desmontar
}, [refreshCurrentCategory]);
```

---

#### R-03 — Decompose Conditional · `Votar.tsx` (PA-1315 y PA-1318)

**Nombre Fowler:** *Decompose Conditional* (descomposición de la condición `isDisabled` y del guard de rol en fragmentos con nombre explícito).

**Objetivo:** La lógica de deshabilitado del botón de voto agrupaba múltiples razones en una sola expresión booleana anónima. Se han añadido dos condiciones nuevas con nombre propio:
- `ownProject` (PA-1315): el usuario logueado es competidor en ese proyecto.
- Guard de rol (PA-1318): el rol del usuario no es `VOTER`, lo que devuelve una pantalla de "sin permiso" antes de renderizar el formulario.

**Beneficios:**
- Cada razón de bloqueo tiene un nombre legible, un mensaje de error propio y un comportamiento visual diferenciado.
- El guard de rol (PA-1318) usa un *early return* que evita renderizar el árbol completo para usuarios sin permisos.

**Código ANTES:**

```tsx
// Votar.tsx — sin comprobación de rol ni de proyecto propio

export default function Votar() {
  // sin useAuth, sin role, sin navigate

  // ...

  return (
    <div className="fade-in">
      {/* formulario visible para cualquier usuario autenticado */}
      {projects.map(proj => {
        const isDisabled = saved[proj.id] || !periodActive
          || (anonLimitReached && !saved[proj.id]) || isPrivate;
        // ...
        return (
          <Card key={proj.id}>
            {/* formulario siempre visible aunque el usuario sea competidor */}
            <Btn disabled={saving[proj.id] || isDisabled}>Enviar Voto</Btn>
          </Card>
        );
      })}
    </div>
  );
}
```

**Código DESPUÉS:**

```tsx
// Votar.tsx — con guard de rol, detección de proyecto propio y aviso previo al formulario

export default function Votar() {
  const { role, currentUser } = useAuth();
  const navigate = useNavigate();

  // PA-1315: IDs de competidores precargados por proyecto
  const [projectCompIds, setProjectCompIds] = useState<Record<number, number[]>>({});

  // PA-1315: precarga de IDs de competidores por proyecto
  useEffect(() => {
    if (projects.length === 0) return;
    projects.forEach(p => {
      getProjectCompetitorIds(p.id)
        .then(ids => setProjectCompIds(prev => ({ ...prev, [p.id]: ids })))
        .catch(() => {});
    });
  }, [projects]);

  // PA-1315: helper con nombre explícito
  const isOwnProject = (proj: Project): boolean => {
    if (!currentUser?.id) return false;
    return (projectCompIds[proj.id] ?? []).includes(currentUser.id);
  };

  // PA-1318: Guard de rol — early return antes de renderizar la pantalla de votación
  if (role !== 'VOTER') {
    return (
      <div className="fade-in flex flex-col items-center justify-center min-h-[60vh] gap-6">
        <div className="p-6 rounded-2xl text-center max-w-sm"
          style={{ background: '#FEE2E2', border: '1.5px solid #FCA5A5' }}>
          <p className="text-4xl mb-3">🚫</p>
          <p className="text-base font-bold mb-1" style={{ color: '#991B1B' }}>
            No tienes permiso para votar
          </p>
          <p className="text-sm" style={{ color: '#B91C1C' }}>
            Solo los usuarios con rol <strong>Votante</strong> pueden acceder a esta sección.
            Tu rol actual es <strong>{role}</strong>.
          </p>
        </div>
        <Btn variant="outline" onClick={() => navigate('/')}>Volver al Dashboard</Btn>
      </div>
    );
  }

  return (
    <div className="fade-in">
      {projects.map(proj => {
        const ownProject = isOwnProject(proj);  // PA-1315: nombre explícito
        const isDisabled = saved[proj.id] || !periodActive
          || (anonLimitReached && !saved[proj.id]) || isPrivate || ownProject;

        return (
          <Card key={proj.id}>
            {/* PA-1315: aviso visible antes del botón — el formulario está bloqueado */}
            {ownProject && (
              <div className="mb-3 p-3 rounded-xl text-sm flex items-start gap-2"
                style={{ background: '#FEF3C7', border: '1.5px solid #FDE68A' }}>
                <span>⚠️</span>
                <p style={{ color: '#92400E' }}>
                  <strong>No puedes votar tu propio proyecto.</strong>
                </p>
              </div>
            )}
            <Btn disabled={saving[proj.id] || isDisabled}>
              {ownProject ? '🚫 Proyecto propio' : 'Enviar Voto'}
            </Btn>
          </Card>
        );
      })}
    </div>
  );
}
```

---

### 3.2 Pruebas unitarias implementadas

Todas las pruebas se encuentran en:

```
src/test/java/com/votify/service/VotingServiceTest.java
```

Se han añadido **8 nuevos tests** al fichero existente, organizados en cuatro bloques correspondientes a las PAs corregidas.

---

#### Bloque PA-1314 — Guard Clause: voto duplicado

| Test | Qué verifica |
|---|---|
| `create_throwsException_whenDuplicateVote` | `create()` lanza `RuntimeException` con el mensaje `"Ya has votado"` cuando `findExistingVote` devuelve un voto previo. |
| `create_neverSaves_whenDuplicateVote` | Verifica que `votingRepository.save()` **no se llama nunca** cuando se detecta duplicado (`verify(repo, never()).save(...)`). |

```java
@Test
@DisplayName("PA-1314 → create lanza excepción cuando ya existe un voto para la misma combinación")
void create_throwsException_whenDuplicateVote() {
    when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
    when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
    when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

    Voting existingVoting = new Voting(voter, competitor, criterion, 15);
    existingVoting.setId(200L);
    when(votingRepository.findExistingVote(1L, 2L, 3L, null))
            .thenReturn(Optional.of(existingVoting));

    assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 2L, 3L, 10)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Ya has votado");
}

@Test
@DisplayName("PA-1314 → create no guarda nada cuando detecta un voto duplicado")
void create_neverSaves_whenDuplicateVote() {
    when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
    when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
    when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

    Voting existingVoting = new Voting(voter, competitor, criterion, 20);
    existingVoting.setId(201L);
    when(votingRepository.findExistingVote(1L, 2L, 3L, null))
            .thenReturn(Optional.of(existingVoting));

    assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 2L, 3L, 5)));
    verify(votingRepository, never()).save(any(Voting.class));
}
```

---

#### Bloque PA-1315 — Guard Clause: voto propio

| Test | Qué verifica |
|---|---|
| `create_throwsException_whenVoterIsCompetitor` | `create()` lanza excepción con `"propio"` en el mensaje cuando `voterId == competitorId`. |
| `create_neverSaves_whenVoterIsCompetitor` | `save()` nunca se invoca cuando el votante y el competidor tienen el mismo ID. |

```java
@Test
@DisplayName("PA-1315 → create lanza excepción cuando voterId == competitorId (voto propio)")
void create_throwsException_whenVoterIsCompetitor() {
    Voter selfVoter = new Voter("Self", "self@test.com", null);
    selfVoter.setId(5L);
    Competitor selfCompetitor = new Competitor("Self", "self@test.com", null);
    selfCompetitor.setId(5L);

    when(voterRepository.findById(5L)).thenReturn(Optional.of(selfVoter));
    when(competitorRepository.findById(5L)).thenReturn(Optional.of(selfCompetitor));

    assertThatThrownBy(() -> votingService.create(new VotingDto(null, 5L, 5L, 3L, 10)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("propio");
}
```

---

#### Bloque PA-1313 — Guard Clause: periodo cerrado

| Test | Qué verifica |
|---|---|
| `create_throwsException_whenPeriodClosed` | Lanza excepción con `"periodo"` cuando `timeFinal` está en el pasado. |
| `create_throwsException_whenPeriodNotStartedYet` | Lanza excepción cuando `timeInitial` está en el futuro. |

```java
@Test
@DisplayName("PA-1313 → create lanza excepción cuando el periodo de la categoría está cerrado")
void create_throwsException_whenPeriodClosed() {
    Event ev = new Event("Hackathon"); ev.setId(99L);
    Category closedCategory = new Category("Cerrada", ev);
    closedCategory.setId(50L);
    closedCategory.setVotingType(VotingType.POPULAR_VOTE);
    closedCategory.setTimeFinal(new java.util.Date(System.currentTimeMillis() - 60_000));

    when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
    when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
    when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
    when(categoryRepository.findById(50L)).thenReturn(Optional.of(closedCategory));

    assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 2L, 3L, 10, 50L)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("periodo");
}
```

---

#### Bloque PA-1316 — `manuallyModified` en intervención manual

| Test | Qué verifica |
|---|---|
| `update_setsManuallyModifiedTrue_onManualIntervention` | `update()` persiste el voto con `manuallyModified = true` cuando el DTO lo indica. |
| `update_keepsManuallyModifiedFalse_whenNotManual` | `update()` preserva `manuallyModified = false` en votos normales. |

```java
@Test
@DisplayName("PA-1316 → update marca manuallyModified=true al guardar una intervención manual")
void update_setsManuallyModifiedTrue_onManualIntervention() {
    when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));

    Voting manualVoting = new Voting(voter, competitor, criterion, 50);
    manualVoting.setId(100L);
    manualVoting.setManuallyModified(true);
    when(votingRepository.save(any(Voting.class))).thenReturn(manualVoting);

    VotingDto dto = new VotingDto(100L, null, null, null, 50);
    dto.setManuallyModified(true);
    VotingDto result = votingService.update(100L, dto);

    assertThat(result.getManuallyModified()).isTrue();
    verify(votingRepository).save(argThat(v -> Boolean.TRUE.equals(v.getManuallyModified())));
}
```

---

### 3.3 Evidencia de ejecución

**Directorio de tests:**

```
src/test/java/com/votify/service/VotingServiceTest.java     ← 8 tests nuevos + 15 existentes = 23 tests
src/test/java/com/votify/persistence/VotingRepositoryTest.java
```

**Comando ejecutado** (Git Bash, Java 21, 26-04-2026 12:26):

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
mvn test -Dtest="VotingServiceTest" --no-transfer-progress
```

**Salida real de consola:**

```
[INFO] Scanning for projects...
[INFO]
[INFO] ---------------------< com.votify:votify-backend >----------------------
[INFO] Building Votify Backend 1.0.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- compiler:3.11.0:compile (default-compile) @ votify-backend ---
[INFO] Compiling 91 source files with javac [debug release 17] to target\classes
[INFO]
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ votify-backend ---
[INFO] Compiling 12 source files with javac [debug release 17] to target\test-classes
[INFO]
[INFO] --- surefire:3.1.2:test (default-test) @ votify-backend ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.votify.service.VotingServiceTest
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.764 s
       -- in com.votify.service.VotingServiceTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  7.908 s
[INFO] Finished at: 2026-04-26T12:26:22+02:00
[INFO] ------------------------------------------------------------------------
```

**Tests ejecutados (23 en verde — 0 fallos, 0 errores):**

```
✔  findAll → retorna todos los votos
✔  findById → retorna DTO correcto cuando existe
✔  findById → lanza excepción cuando no existe
✔  create → crea y guarda voto con entidades correctas
✔  create → lanza excepción si el votante no existe
✔  create → lanza excepción si el competidor no existe
✔  create → lanza excepción si el criterio no existe
✔  update → modifica puntuación del voto existente
✔  delete → llama a deleteById con el id correcto
✔  update (intervención) → permite cambiar score a 0
✔  create POPULAR_VOTE → permite votar si no ha alcanzado el límite de competidores
✔  create POPULAR_VOTE → rechaza voto si supera el límite de 3 competidores de 5
✔  create POPULAR_VOTE → permite votar al mismo competidor ya votado (no cuenta como nuevo)
✔  create POPULAR_VOTE → rechaza voto si supera el totalPoints configurado
✔  create POPULAR_VOTE → permite voto cuando la categoría no tiene límites configurados
✔  PA-1314 → create lanza excepción cuando ya existe un voto para la misma combinación  ← NUEVO
✔  PA-1314 → create no guarda nada cuando detecta un voto duplicado                     ← NUEVO
✔  PA-1315 → create lanza excepción cuando voterId == competitorId (voto propio)         ← NUEVO
✔  PA-1315 → create no guarda nada cuando el votante intenta votar su propio proyecto    ← NUEVO
✔  PA-1313 → create lanza excepción cuando el periodo de la categoría está cerrado       ← NUEVO
✔  PA-1313 → create lanza excepción cuando el periodo todavía no ha comenzado            ← NUEVO
✔  PA-1316 → update marca manuallyModified=true al guardar una intervención manual       ← NUEVO
✔  PA-1316 → update preserva manuallyModified=false cuando no se indica intervención     ← NUEVO
```

---

### 3.4 Resumen de cambios por PA

| PA | Tipo de cambio | Fichero | Fowler |
|---|---|---|---|
| PA-1314 | Backend | `VotingService.java` | Introduce Guard Clause |
| PA-1313 (tests) | Test unitario | `VotingServiceTest.java` | — |
| PA-1314 (tests) | Test unitario | `VotingServiceTest.java` | — |
| PA-1315 (tests) | Test unitario | `VotingServiceTest.java` | — |
| PA-1316 (tests) | Test unitario | `VotingServiceTest.java` | — |
| PA-1315 | Frontend | `Votar.tsx` | Decompose Conditional |
| PA-1317 | Frontend | `Votar.tsx` | Extract Function |
| PA-1318 | Frontend | `Votar.tsx` | Decompose Conditional |
