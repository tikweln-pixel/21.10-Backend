# Votify Backend — Contexto para Claude

## Descripción del proyecto
API REST Spring Boot 3.2.0 + Java 21 + Spring Data JPA para el sistema de votación Votify.
Conecta al PostgreSQL de Supabase via JDBC (Transaction Pooler, puerto 6543).

## Stack tecnológico
- **Lenguaje:** Java 21
- **Framework:** Spring Boot 3.2.0
- **ORM:** Hibernate / Spring Data JPA
- **BD:** PostgreSQL en Supabase (pooler: `aws-1-eu-west-1.pooler.supabase.com:6543`)
- **Pool de conexiones:** HikariCP (prepareThreshold=0 para pgBouncer)
- **Tests:** JUnit 5, Mockito, @DataJpaTest con H2 en memoria
- **Build:** Maven 3.9

## Arquitectura de capas
```
Frontend React (Vite :5173)
        ↓ HTTP /api/*
Backend Spring Boot (:8080)          ← ESTE REPO
        ↓ JDBC (TCP 6543)
PostgreSQL Supabase (Transaction Pooler)
```

## Estructura del proyecto
```
src/main/java/com/votify/
├── controller/     # REST endpoints (@RestController)
├── service/        # Lógica de negocio
├── persistence/    # Repositorios Spring Data JPA
├── entity/         # Entidades JPA (@Entity)
├── dto/            # Data Transfer Objects
├── config/         # WebConfig, CorsConfig, LocalDataSourceConfig, PostgresUrlDataSourceConfig
└── advice/         # RestExceptionHandler global
```

## Entidades principales y relaciones
```
User (id, name, email)
  ├── Competitor extends User  [@PrimaryKeyJoinColumn(name="user_id")]
  └── Voter extends User       [@PrimaryKeyJoinColumn(name="user_id")]
  (Participant fue eliminada en Sprint 1 — ver ADR-007)

Event (id, name, timeInitial, timeFinal)
  └── Category (id, name, votingType, timeInitial, timeFinal)
        └── Criterion (id, name, weight)

Project (id, name, eventId)
  └── @ManyToMany Competitor

EventParticipation (eventId, userId, categoryId, role: COMPETITOR|VOTER)
  └── UNIQUE(event_id, user_id, category_id)

Voting (id, voter, competitor, criterion, category, score, manuallyModified)
Comment (id, project, voter, text)
CategoryCriterionPoints (category, criterion, maxPoints)
TimeWindow (category, startTime, endTime)
```

## Endpoints REST principales
| Método | Path | Descripción |
|--------|------|-------------|
| GET | /api/events | Listar eventos |
| POST | /api/events | Crear evento (con categorías) |
| DELETE | /api/events/{id} | Borrar evento (cascade) |
| GET | /api/events/{id}/categories | Categorías de un evento |
| GET | /api/events/{id}/participations | Participaciones de un evento |
| POST | /api/events/{eventId}/competitors/register | Registrar competidor (crea User+Competitor) |
| POST | /api/events/{eventId}/voters/register | Registrar votante |
| GET | /api/categories/{id}/active-voters | Votantes activos en categoría |
| DELETE | /api/categories/{id} | Borrar categoría (cascade) |
| PUT | /api/categories/{id}/criterion-points/bulk | Configurar puntos (suma=100) |
| GET | /api/votings/by-competitors?ids=1,2,3 | Votos por competidores |
| GET | /api/votings/by-voter-competitor | Votos de voter+competitor |
| GET | /api/projects/{id}/comments | Comentarios de un proyecto |
| GET | /api/competitors/{userId}/comments | Comentarios recibidos por competidor |
| GET | /api/projects/{id}/competitors | IDs de competidores en proyecto |
| GET | /api/actuator/health | Health check |

## Configuración local
- **Perfil activo:** `local` (via `spring.profiles.active=local` en `application.properties`)
- **Credenciales:** `src/main/resources/application-local.properties` (gitignored)
- **DataSource programático:** `config/LocalDataSourceConfig.java` (@Profile("local"), @Primary)
- **Para arrancar:** `mvn clean spring-boot:run` (desde Windows PowerShell con Java 21)

## Variables de entorno en producción
| Variable | Descripción |
|----------|-------------|
| SPRING_DATASOURCE_URL | URL JDBC completa |
| SPRING_DATASOURCE_USERNAME | Usuario Supabase pooler |
| SPRING_DATASOURCE_PASSWORD | Contraseña BD |
| PORT | Puerto HTTP (default 8080) |
| SPRING_PROFILES_ACTIVE | Perfil Spring (production en Railway/Render) |

## Decisiones de arquitectura (ADRs)
- **ADR-001:** Spring Boot como API REST en lugar de acceso directo desde frontend a Supabase
- **ADR-002:** Herencia JPA (JOINED) User→Competitor/Voter + EventParticipation para roles (⚠️ ver ADR-007)
- **ADR-006:** Factory Method en Votify
- **ADR-007:** Eliminación de `Participant` — jerarquía simplificada a User→Competitor/Voter (Sprint 1)
- **ADR-003:** Validación puntos criterio: endpoint individual (≤100) + bulk (=100 exacto)
- **ADR-004:** H2 en memoria para tests @DataJpaTest (no TestContainers en Sprint 0)
- **ADR-005:** Migración frontend de Supabase directo a Spring Boot REST (Sprint 1)

## Tests
- **98 tests** en total (tras Sprint 1): 73 unitarios (Mockito) + 25 integración (@DataJpaTest H2)
- Ejecutar: `mvn test`
- Ejecutar sin tests: `mvn spring-boot:run -DskipTests`

## Reglas de negocio clave
- Suma de `maxPoints` de todos los criterios de una categoría debe ser exactamente 100
- `EventParticipation` UNIQUE(event_id, user_id, category_id) — un usuario, un rol por categoría y evento
- `Voting` UNIQUE(voter_id, competitor_id, criterion_id) — un voto por combinación
- `prepareThreshold=0` en HikariCP es obligatorio para pgBouncer (Transaction Pooler)

## Comandos útiles
```bash
# Arrancar (Windows PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn clean spring-boot:run

# Solo tests
mvn test

# Build JAR
mvn clean package -DskipTests

# Docker
docker build -t votify-backend .
docker run -e SPRING_DATASOURCE_URL=... -e SPRING_DATASOURCE_USERNAME=... -e SPRING_DATASOURCE_PASSWORD=... -p 8080:8080 votify-backend
```
