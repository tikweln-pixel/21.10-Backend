# ADR-002: Herencia JPA (JOINED) para la jerarquía User → Participant → Competitor/Voter

**Fecha:** 21-03-2026
**Sprint:** S0
**Estado:** ⚠️ Parcialmente supersedido por ADR-007 (09-04-2026)

> **Nota (S1):** La jerarquía `User → Participant → Competitor/Voter` documentada
> aquí fue simplificada en Sprint 1. `Participant` fue eliminada como clase
> intermedia vacía. La jerarquía actual es `User → Competitor` y `User → Voter`
> directamente. La decisión de composición de roles via `EventParticipation`
> **no cambia**. Ver **ADR-007** para el detalle completo.

---

### 1) Contexto

El modelo de dominio de Votify establece explícitamente que los roles (Jurado, Votante, Experto, Competidor) deben modelarse por **composición**, NO por herencia desde Participante. Esta regla arquitectónica inamovible existe para evitar el problema del "god class" y facilitar que un mismo usuario tenga múltiples roles en distintas categorías de un evento.

En la implementación JPA del Sprint 0, el equipo de backend tomó la decisión de usar herencia de tablas para representar la jerarquía `User → Participant → Competitor` y `User → Participant → Voter`. Adicionalmente, se creó la entidad `EventParticipation` con un enum `ParticipationRole` (COMPETITOR, VOTER) para gestionar qué rol tiene cada usuario en cada categoría de cada evento.

Esta situación crea una tensión entre la implementación JPA (herencia) y la regla de diseño (composición), que este ADR documenta y justifica.

### 2) Opciones consideradas

- **Opción A:** Composición pura — `User` con una tabla `user_roles` que asocia usuario + evento + categoría + tipo de rol. Sin jerarquía de herencia.
- **Opción B:** Herencia JPA JOINED — `Participant extends User`, `Competitor extends Participant`, `Voter extends Participant` con `@PrimaryKeyJoinColumn`. Tablas separadas por subclase.
- **Opción C:** Herencia JPA SINGLE_TABLE — toda la jerarquía en una sola tabla con columna discriminadora. Más simple pero con columnas nulas.
- **Opción D:** Híbrido — herencia JPA para la jerarquía de personas (User/Participant) + tabla `EventParticipation` con enum de rol para resolver el rol concreto por evento/categoría.

### 3) Criterios de decisión

- Mapeo con el diagrama de clases UML entregado (que sí muestra herencia).
- Facilidad de implementación con Spring Data JPA y Hibernate.
- Capacidad de que un usuario tenga roles distintos en distintas categorías del mismo evento.
- Mantenibilidad y testabilidad.

### 4) Decisión tomada

Se elige la **Opción D (híbrido)**: se mantiene la herencia JPA (JOINED) para la jerarquía `User → Participant → Competitor/Voter` porque el diagrama de clases UML entregado la refleja así, y Hibernate la gestiona de forma natural con `@PrimaryKeyJoinColumn`.

Para cumplir el requisito de roles por composición, se añade la entidad `EventParticipation` con el enum `ParticipationRole` (COMPETITOR, VOTER). Esta tabla registra qué rol tiene cada `User` en cada `Category` de cada `Event`, con constraint de unicidad `(event_id, user_id, category_id)`. Esta tabla es la que implementa la composición de roles descrita en la arquitectura del sistema.

La jerarquía `Competitor extends Participant` se usa principalmente para que `Competitor` pueda tener el método `createProjectForEvent()`, no como mecanismo de resolución de rol en tiempo de ejecución.

### 5) Consecuencias

- **Positivas:**
  - `EventParticipation` resuelve correctamente la composición de roles: un usuario puede ser COMPETITOR en una categoría y VOTER en otra del mismo evento.
  - La constraint `UNIQUE(event_id, user_id, category_id)` evita duplicados a nivel de BD.
  - Tests unitarios e integración validan el registro, duplicados y validación de categoría perteneciente al evento.

- **Negativas / trade-offs:**
  - Hay dos mecanismos de "rol" en el sistema: la jerarquía de clases (`Competitor`, `Voter`) y la tabla `EventParticipation`. Esto puede causar confusión: en `VotingService`, un voto referencia a `Voter` (entidad), mientras que en `EventParticipationService` el rol se gestiona con el enum. Son capas independientes que deben mantenerse sincronizadas.
  - La herencia JOINED genera JOINs adicionales en las queries (users → participants → competitors), con impacto en rendimiento a escala.

- **Riesgos y mitigaciones:**
  - Riesgo: inconsistencia entre la entidad `Competitor` y la participación `EventParticipation` con rol COMPETITOR. Mitigación: en futuros sprints, evaluar migrar a composición pura si la deuda técnica crece (ver ADR-001 para el contexto académico que motivó estas decisiones).

### 6) Evidencia

- `Participant.java`: `@PrimaryKeyJoinColumn(name = "user_id")` — herencia JOINED.
- `Competitor.java`: `@PrimaryKeyJoinColumn(name = "participant_id")` — herencia JOINED.
- `EventParticipation.java`: tabla con `role` enum + constraint `UNIQUE(event_id, user_id, category_id)`.
- `ParticipationRole.java`: enum `{COMPETITOR, VOTER}`.
- Tests: `EventParticipationServiceTest` y `EventParticipationRepositoryTest` (25 tests en total).
- Commit: `efaa335 Add event participation with categories`.
- ⚠️ Ver AI Usage Log Sprint S0, sección 4, para el debate IA sobre composición vs. herencia.
