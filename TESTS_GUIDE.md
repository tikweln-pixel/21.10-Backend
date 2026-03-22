# Votify — Guía de Tests

### Backend Spring Boot · JUnit 5 + Mockito + H2

---

## 📁 Estructura de los tests

```
src/test/
├── java/com/votify/
│   ├── service/                        ← Tests UNITARIOS (Mockito, sin BD)
│   │   ├── CriterionServiceTest.java       9 tests  — CRUD criterios
│   │   ├── EventServiceTest.java           9 tests  — CRUD eventos
│   │   ├── CategoryServiceTest.java       16 tests  — CRUD + puntos + validación fechas
│   │   ├── VotingServiceTest.java         10 tests  — CRUD votos + intervención manual
│   │   ├── ProjectServiceTest.java         9 tests  — CRUD proyectos + comentarios
│   │   └── EventParticipationServiceTest.java  8 tests  — Registro competidores/votantes
│   │
│   └── persistence/                    ← Tests de INTEGRACIÓN (@DataJpaTest + H2)
│       ├── CategoryRepositoryTest.java     9 tests  — Consultas por evento, CRUD
│       ├── VotingRepositoryTest.java       8 tests  — CRUD votos en BD
│       └── EventParticipationRepositoryTest.java  8 tests  — Queries personalizadas
│
└── resources/
    └── application-test.properties     ← H2 en memoria (no necesita Supabase)
```

> **Total: 89 tests** distribuidos en 6 clases de servicio (unit tests) y 3 clases de repositorio (integration tests).

### ***en scr/main/resources/aplication.properties hay que cambiar a true para que ejecute test votify.test.category=false***

---

## ▶️ Cómo ejecutar los tests

### Opción A — Terminal (requiere Maven instalado)

```bash
# Entrar a la carpeta del backend
cd 21.03-Backend

# Ejecutar TODOS los tests
mvn test

# Ejecutar solo los tests de servicio
mvn test -Dtest="*ServiceTest"

# Ejecutar solo los tests de repositorio
mvn test -Dtest="*RepositoryTest"

# Ejecutar una clase específica
mvn test -Dtest=VotingServiceTest

# Ejecutar un test específico
mvn test -Dtest=VotingServiceTest#create_savesVotingWithCorrectEntities

# Ver output detallado
mvn test -pl . --no-transfer-progress

# Generar reporte HTML en target/surefire-reports/
mvn surefire-report:report

# Test recomendado por el último cambio de refactor
mvn test -Dtest=CategoryServiceTest -q
```

### Opción B — IntelliJ IDEA (recomendado para universidad)

1. Abre `21.03-Backend` como proyecto en IntelliJ
2. Clic derecho sobre la carpeta `src/test/java` → **Run All Tests**
3. O clic derecho sobre un archivo de test → **Run 'NombreTest'**
4. O clic en el icono ▶️ junto a un método `@Test` individual

### Opción C — VS Code con Extension Pack for Java

1. Instala la extensión **Extension Pack for Java** en VS Code
2. Abre la carpeta `21.03-Backend`
3. Clic en el icono del matraz 🧪 en la barra lateral izquierda
4. Clic en ▶️ para ejecutar todos los tests

---

## 🧪 Tipos de tests utilizados

### Tests Unitarios (`service/`) — Mockito

- No necesitan base de datos
- Usan `@Mock` para simular repositorios
- Se ejecutan en milisegundos
- Comprueban la **lógica de negocio** de los servicios

```java
@ExtendWith(MockitoExtension.class)  // Activa Mockito
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;  // Mock del repositorio
    @InjectMocks CategoryService categoryService; // Inyecta los mocks

    @Test
    void create_savesAndReturnsDto() {
        // Given
        when(categoryRepository.save(any())).thenReturn(savedCategory);
        // When
        CategoryDto result = categoryService.create(dto);
        // Then
        assertThat(result.getName()).isEqualTo("Jurado Experto");
        verify(categoryRepository).save(any()); // verifica que se llamó
    }
}
```

### Tests de Integración (`persistence/`) — @DataJpaTest + H2

- Usan base de datos H2 en memoria (no necesita Supabase)
- Comprueban que las **queries JPA** funcionan correctamente
- Usan `TestEntityManager` para preparar datos

```java
@DataJpaTest                    // Levanta solo el contexto JPA
@ActiveProfiles("test")         // Usa application-test.properties (H2)
class CategoryRepositoryTest {

    @Autowired TestEntityManager em;           // Para persistir datos de prueba
    @Autowired CategoryRepository repo;        // El repositorio a testear

    @Test
    void findByEventId_returnsOnlyCategoriesOfGivenEvent() {
        // La BD H2 se limpia entre tests automáticamente
        List<Category> result = repo.findByEventId(event.getId());
        assertThat(result).hasSize(2);
    }
}
```

---

## ✅ Qué cubre cada test

### `CategoryServiceTest` (16 tests)

| Test                                                          | Sprint 1 |
| ------------------------------------------------------------- | -------- |
| `findAll_returnsAllCategories`                              | Req. 2   |
| `findByEventId_returnsOnlyCategoriesOfEvent`                | Req. 2   |
| `findById_returnsDto`                                       | Req. 2   |
| `findById_throwsException_whenNotFound`                     | Req. 2   |
| `createForEvent_createsCategoryLinkedToEvent`               | Req. 2   |
| `createForEvent_throwsException_whenEventNotFound`          | Req. 2   |
| `setVotingType_setsJuryExpert`                              | Req. 2   |
| `setVotingType_setsPopularVote`                             | Req. 2   |
| `setCriterionPointsBulk_savesPointsForEachCriterion`        | Req. 10 (Suma=100) |
| `setCriterionPointsBulk_throwsException_whenNegativePoints` | Req. 10  |
| `setCriterionPointsBulk_throwsException_whenSumIsNot100`    | Req. 10 (Suma≠100) |
| `setCriterionPoints_throwsException_whenExceeds100`         | Req. 10 (Excede100)|
| `setTimeInitial_throwsException_whenBeforeEventStart`       | Req. 5   |
| `setTimeFinal_throwsException_whenEndBeforeStart`           | Req. 5   |

### `VotingServiceTest` (10 tests)

| Test                                              | Sprint 1                       |
| ------------------------------------------------- | ------------------------------ |
| `create_savesVotingWithCorrectEntities`         | Req. 7                         |
| `create_throwsException_whenVoterNotFound`      | Req. 7                         |
| `create_throwsException_whenCompetitorNotFound` | Req. 7                         |
| `create_throwsException_whenCriterionNotFound`  | Req. 7                         |
| `update_changesScore`                           | Req. 9 — Intervención Manual |
| `update_allowsScoreZero`                        | Req. 9 — Intervención Manual |
| `delete_callsDeleteById`                        | Req. 9 — Intervención Manual |

### `EventParticipationServiceTest` (8 tests)

| Test                                                                       | Sprint 1 |
| -------------------------------------------------------------------------- | -------- |
| `registerParticipation_registersCompetitor`                              | Req. 4   |
| `registerParticipation_registersVoter`                                   | Req. 4   |
| `registerParticipation_throwsException_whenCategoryIsNull`               | Req. 4   |
| `registerParticipation_throwsException_whenAlreadyRegistered`            | Req. 4   |
| `registerParticipation_throwsException_whenCategoryDoesNotBelongToEvent` | Req. 4   |
| `removeParticipation_deletesParticipation`                               | Req. 9   |

---

## 🔴 Interpretación de fallos comunes

| Error                                       | Causa                                | Solución                                                            |
| ------------------------------------------- | ------------------------------------ | -------------------------------------------------------------------- |
| `NullPointerException` en test            | Mock no configurado                  | Añade `when(...)` para ese mock                                   |
| `EntityNotFoundException` en @DataJpaTest | Entidad no persistida en `setUp()` | Llama `em.persist()` y `em.flush()`                              |
| `DataIntegrityViolationException`         | Email duplicado en H2                | Usa emails únicos por test                                          |
| `Could not autowire`                      | H2 no en classpath                   | Verifica que `h2` esté en `pom.xml` con `scope test`          |
| `Table not found` en H2                   | JPA no creó la tabla                | Verifica `ddl-auto=create-drop` en `application-test.properties` |

---

## 📊 Reporte de cobertura (JaCoCo)

Para añadir cobertura de código, agrega al `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

Luego ejecuta `mvn test` y abre `target/site/jacoco/index.html` en el navegador.

---

## 💡 Añadir nuevos tests

Para añadir un test nuevo, sigue el patrón:

```java
@Test
@DisplayName("nombreMetodo → comportamiento esperado cuando condición")
void nombreMetodo_comportamientoEsperado_cuandoCondicion() {
    // Given — preparación
    when(repository.findById(1L)).thenReturn(Optional.of(entidad));

    // When — acción
    ResultDto result = service.miMetodo(1L);

    // Then — verificación
    assertThat(result.getCampo()).isEqualTo(valorEsperado);
    verify(repository, times(1)).findById(1L);
}
```

---

## 🛠️ Tecnologías de testing

| Librería   | Versión                     | Uso                                        |
| ----------- | ---------------------------- | ------------------------------------------ |
| JUnit 5     | via Spring Boot 3.2          | Framework de tests base                    |
| Mockito     | via spring-boot-starter-test | Mocks de dependencias                      |
| AssertJ     | via spring-boot-starter-test | Assertions fluidas                         |
| H2          | 2.x                          | Base de datos en memoria para @DataJpaTest |
| Spring Test | via spring-boot-starter-test | TestEntityManager, @DataJpaTest            |
