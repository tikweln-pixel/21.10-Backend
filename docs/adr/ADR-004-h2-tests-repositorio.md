# ADR-004: H2 en memoria para tests de repositorio (@DataJpaTest) en lugar de TestContainers/PostgreSQL

**Fecha:** 21-03-2026
**Sprint:** S0
**Estado:** Aprobado

---

### 1) Contexto

El proyecto tiene 3 clases de tests de integración de repositorio (`CategoryRepositoryTest`, `VotingRepositoryTest`, `EventParticipationRepositoryTest`) que usan `@DataJpaTest`. Estos tests levantan el contexto JPA real y ejecutan queries contra una base de datos. Hay que decidir qué BD usar en los tests para validar el comportamiento de las queries derivadas de Spring Data.

En producción, la BD es PostgreSQL (Supabase). H2 es un motor SQL en memoria escrito en Java, compatible con el modo PostgreSQL de forma parcial.

### 2) Opciones consideradas

- **Opción A:** H2 en memoria con modo de compatibilidad PostgreSQL (`MODE=PostgreSQL`) — configurado en `application-test.properties`.
- **Opción B:** TestContainers con imagen Docker de PostgreSQL — levanta un contenedor PostgreSQL real por cada ejecución de tests.
- **Opción C:** Apuntar directamente a la BD de Supabase en tests — usar la conexión real de `application.properties`.

### 3) Criterios de decisión

- Velocidad de ejecución: los tests deben correr rápido en local y en CI.
- Fidelidad con producción: idealmente los tests usan el mismo motor SQL.
- Dependencias externas: sin conexión de red ni Docker obligatorio.
- Complejidad de configuración en Sprint 0.

### 4) Decisión tomada

Se elige la **Opción A**: H2 en memoria con `application-test.properties` (`spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL`). Es la opción más simple, sin dependencias externas, y permite que los tests corran offline y rápido.

La Opción C (BD real de Supabase) se descarta explícitamente: ejecutar tests contra la BD de producción podría corromper datos reales y añade latencia de red. La Opción B (TestContainers) sería más fiel a producción pero requiere Docker y añade tiempo de arranque; se deja como mejora futura para cuando las queries sean más complejas o aparezcan problemas de compatibilidad H2/PostgreSQL.

### 5) Consecuencias

- **Positivas:**
  - Tests de integración corren en < 10 segundos sin red ni Docker.
  - Configuración mínima: solo añadir `h2` en scope `test` al `pom.xml` y `application-test.properties`.
  - El modo `MODE=PostgreSQL` de H2 soporta la mayoría de sintaxis usada (queries derivadas de nombre de método, constraints UNIQUE, enums almacenados como STRING).

- **Negativas / trade-offs:**
  - H2 no soporta al 100% las funciones específicas de PostgreSQL (ej. `jsonb`, arrays, `ON CONFLICT DO UPDATE`). Si en futuros sprints se añaden estas features, los tests de repositorio deberán migrar a TestContainers.
  - Las columnas de tipo `@Enumerated(EnumType.STRING)` funcionan, pero tipos más avanzados como `@Type(PostgreSQLEnumType)` de Hypersistence no son compatibles con H2.

- **Riesgos y mitigaciones:**
  - Riesgo: un test pase en H2 pero falle en PostgreSQL por diferencias de SQL dialect. Mitigación: en Sprint 1, considerar añadir TestContainers para al menos una clase de tests crítica (ej. `VotingRepositoryTest`) como smoke test de compatibilidad.

### 6) Evidencia

- `pom.xml`: `<artifactId>h2</artifactId> <scope>test</scope>`.
- `src/test/resources/application-test.properties`: configuración H2 con modo PostgreSQL.
- 25 tests de repositorio pasan correctamente (9 + 8 + 8).
- Commit: `037b071 test: 83 tests unitarios y de integración`.
