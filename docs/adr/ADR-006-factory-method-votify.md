# ADR-006: Aplicación del patrón Factory Method (GoF) en el backend de Votify

**Fecha:** 07-04-2026 (propuesto) / 11-04-2026 (implementación parcial)
**Sprint:** S1
**Estado:** Implementado parcialmente — EvaluacionCreator completado (Sprint 1)

---

### 1) Contexto

Durante el Sprint 1, el análisis de diseño del backend reveló que las tres familias de objetos
principales — `Votacion`, `Evaluacion` y `Rol` — acumulan lógica de tipo condicional (`if/else`
o `switch`) dentro de sus propias clases de servicio. Esto viola el principio Abierto/Cerrado (OCP)
y el principio de Inversión de Dependencias (DIP), ambos parte de los SOLID.

El problema concreto es el siguiente:

**En `VotingService`** — la normalización del score varía por `VotingType`:
```java
// Situación actual (implícita en el servicio)
if (category.getVotingType() == VotingType.JURY_EXPERT) {
    score = rawScore * weightedByPesos();
} else if (category.getVotingType() == VotingType.POPULAR_VOTE) {
    score = rawScore * 0.85;
}
```

**En `CategoryService`** — la validación de puntos difiere según el tipo de categoría:
cada vez que se añade un tipo nuevo hay que abrir `CategoryService` y añadir ramas.

**En el dominio de `Evaluacion`** — el modelo define seis tipos
(`NUMERICO`, `CHECKLIST`, `RUBRICA`, `COMENTARIO`, `AUDIO`, `VIDEO`), cada uno con
lógica de cálculo de score distinta, actualmente mezclada o pendiente de implementar.

El lab de diseño del Sprint 1 (`lab_factory_method_votify.pdf`) propone exactamente este
patrón para Votify como ejercicio académico. Esta ADR documenta la decisión de adoptarlo
en el backend real.

---

### 2) Opciones consideradas

- **Opción A:** Mantener el diseño actual — `if/else` en servicios o en las propias entidades.
  Bajo coste de implementación ahora, pero crece mal con cada nuevo tipo.

- **Opción B:** Aplicar Factory Method (GoF) completo — jerarquía de `Creator` abstracto +
  `ConcreteCreator` por tipo + `Product` abstracto + `ConcreteProduct` por tipo.
  Mayor número de clases, pero cada una tiene una única responsabilidad y el código existente
  no se toca al añadir tipos nuevos.

- **Opción C:** Aplicar Simple Factory (no-GoF) — una clase `VotacionFactory` con método
  estático y `switch`. Centraliza la construcción pero sigue violando OCP si la switch crece.

---

### 3) Criterios de decisión

- **OCP (Open/Closed Principle):** añadir un tipo nuevo no debe modificar código existente.
- **SRP (Single Responsibility):** cada clase debe tener una única razón para cambiar.
- **Testabilidad:** cada `Creator` y `Product` concreto debe ser testable de forma aislada.
- **Requisito académico:** el lab del Sprint 1 exige la versión GoF completa del patrón.
- **Coste/beneficio:** el sistema tiene variabilidad real y prevista en los tres dominios
  (`VotingType`, tipos de `Evaluacion`, `ParticipationRole`), lo que justifica el coste.

---

### 4) Decisión tomada

Se elige la **Opción B**: Factory Method GoF en tres dominios del backend, por orden de
prioridad de implementación:

#### 4.1 `EvaluacionCreator` — prioridad alta — **IMPLEMENTADO (11-04-2026)**

El dominio `Evaluacion_multicriterio` tiene seis tipos con lógica `calcularScore()` distinta.
Es el caso más directo y de mayor impacto.

**Decisiones de implementación:**
- **JPA:** `SINGLE_TABLE` con `@DiscriminatorColumn(name = "tipo")` — los 6 subtipos difieren
  en comportamiento (`calcularScore()`), no en columnas. Evita 6 tablas JOIN extra.
- **Datos tipo-específicos:** columna `datos` (TEXT) con JSON. Cada subtipo parsea su estructura
  en `calcularScore()` via Jackson `ObjectMapper`.
- **Client del patrón:** `EvaluacionService` usa `Map<TipoEvaluacion, EvaluacionCreator>` en lugar
  de if/else para seleccionar el Creator concreto.

```
entity/
  Evaluacion.java            (abstracto — Product, @Entity SINGLE_TABLE)
    EvaluacionNumerica.java     (ConcreteProduct — score = sum(valores))
    EvaluacionChecklist.java    (ConcreteProduct — score = %checked × 100)
    EvaluacionRubrica.java      (ConcreteProduct — score = avg(nivel/max) × 100)
    EvaluacionComentario.java   (ConcreteProduct — score = null)
    EvaluacionAudio.java        (ConcreteProduct — score = scoreManual o null)
    EvaluacionVideo.java        (ConcreteProduct — score = scoreManual o null)

service/factory/evaluacion/
  EvaluacionCreator.java           (abstracto — Creator)
    EvaluacionNumericaCreator.java     (ConcreteCreator)
    EvaluacionChecklistCreator.java    (ConcreteCreator)
    EvaluacionRubricaCreator.java      (ConcreteCreator)
    EvaluacionComentarioCreator.java   (ConcreteCreator)
    EvaluacionAudioCreator.java        (ConcreteCreator)
    EvaluacionVideoCreator.java        (ConcreteCreator)
```

Método abstracto en `Creator`: `Evaluacion create(EvaluacionDTO dto)`
Método compartido en `Creator`: `Evaluacion createAndValidate(EvaluacionDTO dto)` (valida peso ≥ 0
antes de delegar en `create()`).

#### 4.2 `VotacionCreator` — prioridad alta

Mapeo directo al lab: `JURY_EXPERT` → `VotacionExperto`, `POPULAR_VOTE` → `VotacionPopular`.
El único `if/else` restante en el cliente selecciona el `Creator` (1-2 líneas en el servicio).

```
domain/
  Votacion (abstracto — Product)
    VotacionExperto  → normalizedScore() = rawScore * weightByCriteria()
    VotacionPopular  → normalizedScore() = rawScore * 0.85

factory/
  VotacionCreator (abstracto — Creator)
    VotacionExpertoCreator
    VotacionPopularCreator
```

#### 4.3 `RolCreator` — prioridad media (Sprint 2-3)

Para la generación de permisos y certificados por rol. Complementa la composición ya
implementada con `EventParticipation`.

```
domain/
  Rol (abstracto — Product)
    RolJurado      → permisos: [EVALUAR, VER_RESULTADOS, ELEGIR_PREMIO]
    RolVotante     → permisos: [VOTAR, VER_RANKING]
    RolExperto     → permisos: [EVALUAR_MULTICRITERIO, ESCRIBIR_COMENTARIO]
    RolCompetidor  → permisos: [VER_DASHBOARD, VER_HOJA_RUTA]

factory/
  RolCreator (abstracto — Creator)
    RolJuradoCreator
    RolVotanteCreator
    RolExpertoCreator
    RolCompetidorCreator
```

#### 4.4 `ParticipantFactory` en el frontend — prioridad media

El `client.ts` del frontend tiene tres versiones inconsistentes de mapping de `Participant`
desde la API. Se centralizan en `src/factories/participantFactory.ts`:

```typescript
export const ParticipantFactory = {
  fromCompetitorApi(raw: any): Participant { ... },
  fromVoterApi(raw: any): Participant { ... },
  fromJuryApi(raw: any): Participant { ... },
}
```

No es GoF puro (TypeScript no tiene herencia de clases abstractas igual que Java), pero aplica
el mismo principio: una sola responsabilidad de construcción, extensible sin tocar las páginas.

---

### 5) Lo que NO se aplica

| Entidad | Razón para no aplicar |
|---------|----------------------|
| `Event` | No hay variación de comportamiento entre tipos de evento en el MVP actual |
| `Criterion` | Todos calculan igual: `peso × valor`. No hay polimorfismo real |
| `Project` | La diferencia entre proyectos es de datos, no de comportamiento |
| `Category` | El `VotingType` es un atributo que ya resuelve el Factory de `Votacion` |

Regla aplicada: Factory Method solo donde el **comportamiento** varía por tipo, no solo los datos.

---

### 6) Consecuencias

**Positivas:**
- Añadir `SponsorVote` o `EvaluacionVideo` → 2 ficheros nuevos, 0 ficheros modificados.
- Cada `Creator` y `Product` concreto se prueba de forma completamente aislada.
- Elimina la deuda técnica de `if/else` dispersos en los servicios.
- Cumple el requisito académico del lab de Sprint 1.

**Negativas / trade-offs:**
- Aumenta el número de clases: por cada tipo nuevo, 2 clases (Product + Creator).
- Requiere refactorizar `VotingService` y la futura `EvaluacionService` para que usen los Creators.
- El único `if/else` restante (selección del Creator en el servicio) puede moverse a un Map
  o registro en el futuro si el número de tipos crece mucho.

**Riesgos y mitigaciones:**
- Riesgo: el mapeo JPA de `Evaluacion` con 6 subclases puede requerir estrategia JOINED o
  SINGLE_TABLE. Mitigación: evaluar con el equipo de backend si conviene herencia JPA en
  `Evaluacion` o mantenerla como valor en el DTO. Documentar en ADR posterior si se opta
  por herencia JPA.
- Riesgo: sobrediseño si los tipos de `Evaluacion` nunca se implementan todos en el MVP.
  Mitigación: implementar primero `EvaluacionNumerica` (el caso base) y añadir el resto
  progresivamente en Sprint 2-3.

---

### 7) Evidencia y referencias

- Lab de referencia: `lab_factory_method_votify.pdf` (Sprint 1, DDS)
- Patrón original: GoF "Design Patterns" (Gang of Four, 1994) — sección Factory Method
- Análisis completo de oportunidades: sesión Claude Code 07-04-2026 (ver AI log Sprint S1,
  sección Factory Method)
- ADR relacionados: ADR-002 (herencia JPA — afecta a cómo se implementa `EvaluacionCreator`),
  ADR-003 (validación puntos — responsabilidad que pasa al `Creator` de evaluación)
- Estructura final prevista:

**Estructura real implementada (11-04-2026):**
```
src/main/java/com/votify/
├── entity/                                    ← Products (entidades JPA)
│   ├── Evaluacion.java                        (abstracto — IMPLEMENTADO)
│   ├── EvaluacionNumerica.java                (IMPLEMENTADO)
│   ├── EvaluacionChecklist.java               (IMPLEMENTADO)
│   ├── EvaluacionRubrica.java                 (IMPLEMENTADO)
│   ├── EvaluacionComentario.java              (IMPLEMENTADO)
│   ├── EvaluacionAudio.java                   (IMPLEMENTADO)
│   ├── EvaluacionVideo.java                   (IMPLEMENTADO)
│   └── TipoEvaluacion.java                    (enum — IMPLEMENTADO)
├── dto/
│   └── EvaluacionDto.java                     (IMPLEMENTADO)
├── persistence/
│   └── EvaluacionRepository.java              (IMPLEMENTADO)
├── service/
│   ├── EvaluacionService.java                 (Client del patrón — IMPLEMENTADO)
│   └── factory/
│       ├── participant/                       (Factory existente — Sprint 0)
│       │   ├── ParticipantCreator.java
│       │   ├── CompetitorCreator.java
│       │   └── VoterCreator.java
│       └── evaluacion/                        ← Creators (IMPLEMENTADO)
│           ├── EvaluacionCreator.java         (abstracto)
│           ├── EvaluacionNumericaCreator.java
│           ├── EvaluacionChecklistCreator.java
│           ├── EvaluacionRubricaCreator.java
│           ├── EvaluacionComentarioCreator.java
│           ├── EvaluacionAudioCreator.java
│           └── EvaluacionVideoCreator.java
└── controller/
    └── EvaluacionController.java              (IMPLEMENTADO)
```

**Pendiente de implementación (Sprint 2-3):**
```
├── entity/
│   ├── Votacion.java                          (abstracto — PENDIENTE)
│   ├── VotacionExperto.java                   (PENDIENTE)
│   ├── VotacionPopular.java                   (PENDIENTE)
│   ├── Rol.java                               (abstracto — PENDIENTE)
│   ├── RolJurado.java                         (PENDIENTE)
│   ├── RolVotante.java                        (PENDIENTE)
│   ├── RolExperto.java                        (PENDIENTE)
│   └── RolCompetidor.java                     (PENDIENTE)
└── service/factory/
    ├── votacion/                              (PENDIENTE)
    └── rol/                                   (PENDIENTE)
```
