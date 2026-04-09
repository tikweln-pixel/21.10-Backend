# ADR-007: Eliminación de la clase intermedia `Participant` — simplificación de jerarquía JPA

**Fecha:** 09-04-2026
**Sprint:** S1
**Estado:** Aprobado

---

### 1) Contexto

ADR-002 documentó la decisión de usar herencia JPA JOINED con la jerarquía
`User → Participant → Competitor/Voter`. En esa decisión, `Participant` fue
introducida como clase intermedia para respetar el UML original del proyecto.

Sin embargo, `Participant` nunca tuvo atributos ni comportamiento propios: era
una clase vacía cuya única función era ser un eslabón en la cadena de herencia.
Al mismo tiempo, la entidad `EventParticipation` ya modelaba semánticamente el
concepto de "participación en un evento" con rol, evento y categoría. Tener una
clase `Participant` en la jerarquía generaba confusión conceptual (¿cuál de los
dos representa "participar"?) y un JOIN extra en cada consulta a competidores y
votantes.

---

### 2) Opciones consideradas

- **Opción A (elegida):** Eliminar `Participant`. Que `Competitor` y `Voter`
  extiendan `User` directamente. La participación en eventos se gestiona
  exclusivamente a través de `EventParticipation`.
- **Opción B:** Mantener `Participant` y añadirle al menos un atributo para
  justificar su existencia (ej. `joinedAt`).
- **Opción C:** Eliminar `Participant` y también `Competitor`/`Voter`, dejando
  solo `User` + `EventParticipation` para todo (composición pura).

---

### 3) Criterios de decisión

- Eliminar deuda técnica sin romper funcionalidad existente.
- Mantener la capacidad de que `Competitor` tenga lógica propia
  (`createProjectForEvent`).
- Reducir JOINs innecesarios en las queries de hibernate.
- Coherencia entre nombre de clase y responsabilidad real.

---

### 4) Decisión tomada

**Opción A:** se elimina `Participant` como entidad intermedia.

La jerarquía queda:

```
User  (tabla: users)
 ├── Competitor  (tabla: competitors)  — @PrimaryKeyJoinColumn(name = "user_id")
 └── Voter       (tabla: voters)       — @PrimaryKeyJoinColumn(name = "user_id")
```

`EventParticipation` sigue siendo el mecanismo de composición de roles por
evento y categoría, sin cambios.

Los DTOs se actualizan en consecuencia:
- `CompetitorDto` extiende `UserDto` directamente (antes extendía `ParticipantDto`).
- `VoterDto` extiende `UserDto` directamente.
- `ParticipantDto` se elimina.
- `ParticipantController`, `ParticipantService` y `ParticipantRepository`
  se eliminan (toda la responsabilidad recae en `EventParticipationService` y
  los servicios concretos de cada rol).

---

### 5) Consecuencias

- **Positivas:**
  - Cada query a `Competitor` o `Voter` hace un JOIN menos (users → competitors,
    en vez de users → participants → competitors).
  - La distinción conceptual queda clara: `Competitor`/`Voter` es el tipo de
    usuario, `EventParticipation` es la pertenencia a un evento concreto.
  - Se eliminan ~178 líneas de código sin pérdida de funcionalidad.

- **Negativas / trade-offs:**
  - El UML académico original muestra `Participant` como clase intermedia. La
    implementación ya no es fiel al diagrama entregado. Hay que actualizar el
    diagrama si se presenta en evaluación.
  - Cualquier código del frontend que llamara a `/participants` (ParticipantController
    eliminado) debe migrar a los endpoints correspondientes de
    `EventParticipation` o del rol concreto.

---

### 6) Relación con ADRs anteriores

- **Actualiza ADR-002:** la jerarquía documentada allí (`User → Participant →
  Competitor/Voter`) queda reemplazada por la de este ADR. La tabla
  `participants` desaparece de la BD.
- La decisión de composición de roles via `EventParticipation` (parte central
  de ADR-002) **no cambia** — se mantiene íntegramente.

---

### 7) Evidencia

- Commit: `c3e26ca Eliminación de clase Participant innecesaria`
- Archivos eliminados: `Participant.java`, `ParticipantController.java`,
  `ParticipantDto.java`, `ParticipantService.java`, `ParticipantRepository.java`
- Archivos modificados: `Competitor.java`, `Voter.java`, `CompetitorDto.java`,
  `VoterDto.java`, `EventDto.java`, `ProjectService.java`
- ⚠️ Registrar en AI Usage Log Sprint S1 si se usó IA para validar el impacto
  del cambio.
