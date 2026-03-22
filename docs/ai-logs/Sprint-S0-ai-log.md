# AI Usage Log — Sprint S1

**Sprint:** S1 — Base y configuración del backend con su logica y persistencia
**Periodo:** Febrero–Marzo 2026
**Herramientas usadas en este sprint:** Claude Sonnet (Cowork / Claude.ai), GitHub Copilot (integrado en VS Code/Cursor)

---

### 1) Herramientas usadas

- **Claude Sonnet (claude.ai / Cowork):**

  - Consultas de arquitectura: diseño de la jerarquía de entidades JPA, estrategia de herencia vs. composición de roles.
  - Generación y revisión de tests unitarios (Mockito) y de integración (@DataJpaTest).
  - Redacción de ADRs y documentación técnica.
  - Revisión de lógica de validación en `CategoryService` (puntos por criterio, validación de fechas).
- **GitHub Copilot (Cursor/VS Code):**

  - Autocompletado de boilerplate JPA (getters/setters, constructores, anotaciones `@Entity`, `@ManyToOne`).
  - Sugerencias de nombres de métodos en repositorios Spring Data derivados de query-by-name.
  - Esqueletos de clases de test (setup de `@BeforeEach`, mocks con Mockito).

---

### 2) Prompts clave

- **Prompt 1:** "Tengo una jerarquía User → Participant → Competitor/Voter en JPA con @PrimaryKeyJoinColumn. El diseño del sistema dice que los roles deben ser por composición, no herencia. ¿Cómo puedo mantener la herencia JPA para el mapeo de tablas y aun así implementar la composición de roles por evento y categoría?"
- **Prompt 2:** "Diseña una entidad `EventParticipation` que permita que un mismo usuario tenga diferentes roles (COMPETITOR, VOTER) en distintas categorías de un mismo evento, con constraint de unicidad a nivel de BD."
- **Prompt 3:** "En `CategoryService`, implementa la lógica para que al guardar puntos por criterio de forma individual, la suma de todos los criterios de esa categoría no supere 100. Y un segundo método bulk que exija suma exactamente = 100 al confirmar."
- **Prompt 4:** "Genera los tests unitarios con Mockito para `CategoryService`, cubriendo: CRUD básico, tipo de votación, puntos por criterio (bulk válido, puntos negativos, suma ≠ 100, individual que excede 100), y validación de fechas (categoría fuera del rango del evento)."
- **Prompt 5:** "Configura @DataJpaTest con H2 en memoria para los tests de repositorio de `CategoryRepository`. El proyecto usa PostgreSQL en producción. ¿Qué configuración necesito en application-test.properties y qué limitaciones tiene H2 vs PostgreSQL?"
- **Prompt 6:** "Genera el setup de `EventParticipationRepositoryTest` con @DataJpaTest, un evento con 2 categorías (JURY_EXPERT y POPULAR_VOTE) y 3 participaciones (userComp en catJury como COMPETITOR, userVoter en catJury como VOTER, userComp en catPopular como COMPETITOR). Necesito tests para findByEventId, findByEventIdAndCategoryId, exists, y findByUserId."

---

### 3) Salidas relevantes (resumen corto)

- **Prompt 1 →** La IA propuso mantener la herencia JPA para el mapeo de tablas (estrategia JOINED es la más limpia para esta jerarquía) y añadir una tabla `EventParticipation` separada con enum `ParticipationRole` para la composición de roles. Esta solución híbrida resuelve la tensión entre el diagrama UML y la regla de composición.
- **Prompt 2 →** La IA generó la entidad `EventParticipation` con los campos `event`, `user`, `category`, `role`, y el `@UniqueConstraint(columnNames = {"event_id", "user_id", "category_id"})`. También propuso la validación de que la categoría pertenezca al evento antes de registrar la participación.
- **Prompt 3 →** La IA propuso los dos métodos (`setCriterionPoints` y `setCriterionPointsBulk`) con sus validaciones. En el bulk, propuso usar `deleteByCategoryId` + re-inserción envuelta en `@Transactional`.
- **Prompt 4 →** La IA generó 16 tests para `CategoryServiceTest`, incluyendo los casos límite de validación de puntos. Se detectó un fallo en el test de `setCriterionPoints_throwsException_whenExceeds100` que la IA no había modelado correctamente (sumaba el total incluyendo el criterio que se editaba en lugar de excluirlo).
- **Prompt 5 →** La IA recomendó H2 con `MODE=PostgreSQL` para Sprint 0, advirtiendo que en futuros sprints con queries avanzadas se debería migrar a TestContainers. Proporcionó el `application-test.properties` correcto.
- **Prompt 6 →** La IA generó el setup completo de `EventParticipationRepositoryTest` con los 3 usuarios y 3 participaciones. Los tests pasaron sin modificaciones.

---

### 4) Qué aceptamos y qué rechazamos

- **Aceptado:** Entidad `EventParticipation` con constraint UNIQUE y validación de categoría → por qué: resuelve correctamente la composición de roles y evita duplicados a nivel de BD sin lógica extra en el frontend.
- **Aceptado:** Método `setCriterionPointsBulk` con `deleteByCategoryId` + re-inserción en lugar de upsert individual → por qué: más simple y atómico para el caso de "confirmar toda la configuración de una vez"; el upsert individual queda para el slider en tiempo real.
- **Aceptado:** H2 con `MODE=PostgreSQL` para los tests de repositorio en Sprint 0 → por qué: no hay queries específicas de PostgreSQL aún y la velocidad de ejecución es prioritaria. Se acepta la deuda técnica de migrar a TestContainers cuando sea necesario (documentada en ADR-004).
- **Rechazado/corregido:** El test `setCriterionPoints_throwsException_whenExceeds100` generado por la IA incluía el criterio que se estaba editando en la suma de "otros criterios", lo que causaba que el test fallara. Se corrigió manualmente para que el filtro excluyera el `criterionId` que se está actualizando (`filter(ccp -> !ccp.getCriterion().getId().equals(criterionId))`).
- **Rechazado/corregido:** La IA inicialmente propuso usar `@Autowired` en los campos de los servicios para la inyección de dependencias. Se rechazó en favor de inyección por constructor, que es la práctica recomendada por Spring y facilita los tests unitarios con Mockito.
- **Rechazado:** La IA sugirió usar `@ManyToMany` entre `Project` y `Competitor` con tabla join automática. Se rechazó porque el modelo de dominio requiere control explícito de esa relación; se mantuvo la colección `List<Competitor>` en `Project` con `@ManyToMany` pero con tabla de join controlada.

---

### 5) Cómo lo verificamos

- [X] Tests unitarios: 61 tests unitarios con Mockito (CriterionServiceTest×9, EventServiceTest×9, CategoryServiceTest×16, VotingServiceTest×10, ProjectServiceTest×9, EventParticipationServiceTest×8). Todos pasan (`mvn test`).
- [X] Tests de integración: 25 tests @DataJpaTest con H2 (CategoryRepositoryTest×9, VotingRepositoryTest×8, EventParticipationRepositoryTest×8). Todos pasan.
- [X] Consulta a documentación oficial: Spring Data JPA docs para query derivation; Hibernate docs para estrategia JOINED; H2 docs para `MODE=PostgreSQL`.
- [X] Revisión por pares: el diseño de `EventParticipation` y la lógica de puntos fue revisado por el equipo antes del merge al commit `16031ad`.

---

### 6) Resultado final / decisión humana

El equipo validó y aprobó el diseño híbrido (herencia JPA + `EventParticipation` para composición de roles) documentado en ADR-002. La lógica de validación de puntos (ADR-003) se implementó con los dos métodos (individual + bulk) y está cubierta por 4 tests específicos en `CategoryServiceTest`.

Total de tests al cierre del Sprint 0: **86 tests**, todos en verde.

El uso de IA fue instrumental para acelerar la generación de boilerplate y tests, pero la decisión final sobre el diseño de `EventParticipation`, la corrección del test de puntos, y la estrategia de inyección de dependencias fueron tomadas por el equipo tras revisar y corregir las propuestas.

Referencias en el repo:

- ADR-001: `docs/adr/ADR-001-spring-boot-rest-backend.md`
- ADR-002: `docs/adr/ADR-002-herencia-jpa-vs-composicion-roles.md`
- ADR-003: `docs/adr/ADR-003-validacion-puntos-criterio-100.md`
- ADR-004: `docs/adr/ADR-004-h2-tests-repositorio.md`
- Commits clave: `af98f22`, `efaa335`, `16031ad`, `037b071`
