# ADR-001: Introducción de Spring Boot como API REST en lugar de usar Supabase directamente

**Fecha:** 21-03-2026
**Sprint:** S0
**Estado:** Aprobado

---

### 1) Contexto

El diseño original de Votify (documentado en el skill del proyecto) establecía que Supabase sería la única fuente de verdad y que no habría backend propio en el MVP: el frontend React accedería directamente a la base de datos a través del cliente JS de Supabase.

Durante el Sprint 0, el equipo de backend (asignatura de ingeniería del software) necesitó construir una capa de servidor propia como requisito académico. Esto introduce una arquitectura de tres capas: frontend React → API REST (Spring Boot) → PostgreSQL (Supabase como BD gestionada).

### 2) Opciones consideradas

- **Opción A:** Mantener el diseño original — frontend accede a Supabase JS client directamente, sin backend propio.
- **Opción B:** Implementar una API REST con Spring Boot 3 + JPA que conecte al PostgreSQL de Supabase mediante JDBC.
- **Opción C:** Implementar una API REST con Node.js/Express, más cercana al stack del frontend (JavaScript).

### 3) Criterios de decisión

- Requisito académico: la asignatura de backend exige una API REST en Java.
- Mantenibilidad: separación clara de responsabilidades entre capas.
- Familiaridad del equipo: el equipo de backend trabaja con Java/Spring.
- Reutilización de la BD: se mantiene Supabase como BD PostgreSQL gestionada (solo se cambia el acceso).

### 4) Decisión tomada

Se elige la **Opción B**: API REST con Spring Boot 3.2.0 + Java 21 + Spring Data JPA, conectando al PostgreSQL de Supabase vía JDBC (driver `postgresql`, pool de conexiones de Supabase).

Esta decisión respeta el modelo de dominio diseñado (entidades, relaciones, reglas de negocio) y permite que el frontend React consuma los endpoints REST en lugar de acceder directamente a Supabase. La base de datos sigue siendo el PostgreSQL de Supabase, por lo que Supabase continúa siendo la fuente de verdad de los datos.

### 5) Consecuencias

- **Positivas:**
  - Separación clara de responsabilidades: la lógica de negocio (validación de puntos, periodos de votación, unicidad de participaciones) reside en el backend y no se duplica en el frontend.
  - Testabilidad: se pueden escribir tests unitarios (Mockito) e integración (@DataJpaTest con H2) independientemente del frontend.
  - Control de acceso centralizado: en futuros sprints es más sencillo añadir seguridad (Spring Security) en un único punto.

- **Negativas / trade-offs:**
  - Añade complejidad operacional: hay que desplegar el backend además del frontend.
  - El cliente JS de Supabase (auth, realtime) queda fuera del alcance del backend; si el frontend necesita realtime, deberá gestionar esa suscripción directamente con Supabase.
  - `spring.jpa.hibernate.ddl-auto=update` en producción puede generar migraciones implícitas no controladas (deuda técnica a resolver en sprints posteriores con Flyway o Liquibase).

- **Riesgos y mitigaciones:**
  - Riesgo: divergencia entre el esquema JPA y el esquema real de Supabase. Mitigación: usar `ddl-auto=update` en desarrollo y revisar manualmente las migraciones antes de hacer deploy.
  - Riesgo: las credenciales de BD están en `application.properties` en texto plano. Mitigación: mover a variables de entorno antes del primer despliegue real.

### 6) Evidencia

- `pom.xml`: dependencias `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`.
- `application.properties`: URL de conexión a `pooler.supabase.com` vía JDBC.
- Commit inicial: `af98f22 Add full backend: entities, repositories, DTOs, services, controllers, Spring Boot config`.
