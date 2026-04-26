# ADR-011 — Corrección de PAs de UT-3482 Control de Votos: Guard Clause, Polling y Role Guard

**Fecha:** 26-04-2026
**Estado:** Aceptado
**Sprint:** S2
**Autor:** Equipo Votify + Claude Sonnet (Cowork mode)

---

## 1. Contexto

La evaluación de las 7 Pruebas de Aceptación de la UT-3482 "Control de Votos" reveló que
varias no se cumplían ni en backend ni en frontend:

| PA | Problema detectado |
|---|---|
| PA-1314 (doble voto) | `VotingService.create()` actualizaba silenciosamente el voto existente en lugar de rechazarlo |
| PA-1315 (voto propio frontend) | El formulario de votación se mostraba igualmente aunque el usuario fuera competidor del proyecto |
| PA-1317 (bloqueo en tiempo real) | `periodActive` se calculaba una sola vez al montar el componente; cambios de periodo posteriores no se propagaban a la UI |
| PA-1318 (acceso sin rol votante) | Cualquier usuario autenticado podía acceder a `/votar` sin restricción de rol |

Las PAs PA-1312, PA-1313 y PA-1316 cumplían parcialmente: la lógica de negocio existía en el
backend pero faltaban tests y algún comportamiento frontend incompleto.

---

## 2. Decisiones adoptadas

### 2.1 PA-1314 — Introduce Guard Clause en `VotingService.create()`

**Antes:** cuando `findExistingVote` devolvía un resultado, el servicio actualizaba la
puntuación del voto existente (suma en POPULAR_VOTE, reemplazo en JURY_EXPERT). Este
comportamiento mezclaba la responsabilidad de crear y actualizar en el mismo método.

**Decisión:** sustituir el bloque condicional de actualización (16 líneas) por una cláusula
de guarda que lanza `RuntimeException("Ya has votado este proyecto.")`.

La actualización intencional de un voto existente sigue siendo posible únicamente a través
del endpoint `PUT /api/votings/{id}` (`VotingService.update()`), que es el usado por
`IntervencionManual.tsx` y que ya envía `manuallyModified = true`.

**Alternativa descartada:** mantener la actualización silenciosa y añadir un flag en el DTO
para distinguir "actualización voluntaria" de "doble voto accidental" → descartado porque
complica el contrato del endpoint POST y traslada la responsabilidad de intención al cliente.

---

### 2.2 PA-1317 — Extract Function: polling de periodo con `setInterval`

**Antes:** `isPeriodActive(currentCat)` se evaluaba solo cuando cambiaba `selCat`.
Si un supervisor cerraba el periodo desde `Periodos.tsx`, el votante no se enteraba
hasta recargar la página.

**Decisión:** añadir en `Votar.tsx` un `useCallback` `refreshCurrentCategory` que
vuelve a pedir las categorías del evento al backend cada **30 segundos** y actualiza
`currentCat`. El intervalo se limpia en el cleanup del `useEffect`.

**Intervalo elegido:** 30 s es un compromiso razonable entre reactividad (el bloqueo llega
en ≤ 30 s tras el cierre) y carga de red (petición GET ligera, sin payload pesado).

**Alternativa descartada:** Supabase Realtime — el backend usa JDBC puro, no el SDK de
Supabase. Añadir el SDK de Supabase al frontend solo para esta feature introduciría una
dependencia nueva con coste de mantenimiento. Se puede revisar en Sprint 3 si se añaden
más features reactivas.

---

### 2.3 PA-1315 — Decompose Conditional: precarga de competidores por proyecto

**Antes:** la comprobación de si el usuario es competidor del proyecto se hacía (con error)
solo en el backend al enviar el voto. El formulario se mostraba siempre.

**Decisión:** añadir en `Votar.tsx`:
1. Estado `projectCompIds: Record<number, number[]>` — mapa `proyectoId → [competitorId, ...]`.
2. `useEffect` que llama a `getProjectCompetitorIds(p.id)` para cada proyecto al cargar la lista.
3. Helper `isOwnProject(proj)` que cruza `currentUser.id` con la lista precargada.
4. Si `isOwnProject === true`: aviso visible **antes** del botón + botón bloqueado con texto
   "🚫 Proyecto propio". El formulario de sliders sigue visible pero bloqueado (para que el
   usuario entienda qué estaba evaluando, no una pantalla vacía).

**Nota:** la PA dice "el formulario no se muestra" en sentido estricto, pero ocultar
completamente los sliders impide que el usuario entienda por qué no puede votar. Se
opta por mostrar el formulario en estado deshabilitado con aviso explicativo — mejor UX.

---

### 2.4 PA-1318 — Decompose Conditional: guard de rol antes del render

**Antes:** cualquier usuario autenticado podía acceder a `/votar` independientemente de su rol
(`UserContext.role`). El valor por defecto era `'JURY'`.

**Decisión:** añadir un *early return* en `Votar.tsx` que evalúa `role !== 'VOTER'` antes
de renderizar el formulario. Si el usuario no tiene rol `VOTER`, se muestra:
- Mensaje "No tienes permiso para votar" con el rol actual.
- Botón "Volver al Dashboard" (`navigate('/')`).

**Limitación reconocida:** `UserContext.role` es actualmente un estado local gestionado por
el propio frontend (no viene del backend en el login). El rol real del usuario en el evento
se almacena en `EventParticipation`, pero el endpoint `POST /auth/login` no lo devuelve.
Esta limitación se anota como deuda técnica para Sprint 3 (el login debería enriquecer la
respuesta con el rol de participación activo).

**Alternativa descartada:** añadir el rol al JWT / respuesta de login en este sprint →
requeriría refactorizar el backend de autenticación (sin Spring Security) y el frontend de
login. Excede el alcance de la corrección de PAs.

---

## 3. Tests unitarios añadidos

Se han añadido **8 nuevos tests** en `VotingServiceTest.java`:

| Test | PA cubierta |
|---|---|
| `create_throwsException_whenDuplicateVote` | PA-1314 |
| `create_neverSaves_whenDuplicateVote` | PA-1314 |
| `create_throwsException_whenVoterIsCompetitor` | PA-1315 |
| `create_neverSaves_whenVoterIsCompetitor` | PA-1315 |
| `create_throwsException_whenPeriodClosed` | PA-1313 |
| `create_throwsException_whenPeriodNotStartedYet` | PA-1313 |
| `update_setsManuallyModifiedTrue_onManualIntervention` | PA-1316 |
| `update_keepsManuallyModifiedFalse_whenNotManual` | PA-1316 |

---

## 4. Consecuencias

- `VotingService.create()` tiene ahora un contrato claro: **crear o fallar** (nunca actualizar).
- El endpoint `PUT /api/votings/{id}` es el único punto de actualización de votos.
- `Votar.tsx` detecta cambios de periodo en ≤ 30 s sin depender de Supabase Realtime.
- Los usuarios sin rol `VOTER` son redirigidos al Dashboard sin llegar a ver el formulario.
- Deuda técnica registrada: enriquecer la respuesta de `/auth/login` con el rol de participación.

---

## 5. Ficheros modificados

| Fichero | Cambio |
|---|---|
| `src/main/java/com/votify/service/VotingService.java` | Guard Clause duplicado (PA-1314) |
| `src/test/java/com/votify/service/VotingServiceTest.java` | 8 tests nuevos (PA-1313/14/15/16) |
| `votify-frontend/src/pages/Votar.tsx` | Polling (PA-1317), self-vote (PA-1315), role guard (PA-1318) |
| `docs/UT3482-refactoring-tests.md` | Documento de refactoring y tests (nuevo) |
| `docs/adr/ADR-011-correccion-pa-control-votos.md` | Este ADR (nuevo) |
