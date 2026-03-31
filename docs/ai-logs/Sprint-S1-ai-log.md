# AI Usage Log — Sprint S1

**Sprint:** S1 — Migración frontend + debug conexión backend
**Periodo:** Marzo 2026
**Herramientas usadas en este sprint:** Claude Sonnet (Claude Code / claude.ai), GitHub Copilot (integrado en Cursor)

---

### 1) Herramientas usadas

- **Claude Sonnet (Claude Code — modo agente en terminal):**
  - Diagnóstico y resolución del problema de autenticación con Supabase Transaction Pooler (pgBouncer reportaba `user "postgres"` enmascarando la causa real).
  - Creación de `LocalDataSourceConfig.java` para bypassing programático de la resolución de propiedades Spring.
  - Análisis del flujo de capas del frontend: páginas → `client.ts` → `axios.ts` → proxy Vite → backend.
  - Creación y actualización de `CLAUDE.md` en backend y frontend para contexto de proyecto.
  - Redacción de ADR-005 (migración frontend de Supabase a Spring Boot REST).
  - Actualización de documentación: ADR-001, BACKEND-RESUMEN.md, SKILL.md del proyecto.

- **GitHub Copilot (Cursor):**
  - Autocompletado durante la reescritura de `src/api/client.ts` del frontend.
  - Sugerencias de métodos de repositorio Spring Data en los nuevos repos.

---

### 2) Prompts clave

- **Prompt 1:** "el backend sigue dando error de autenticación `FATAL: password authentication failed for user 'postgres'` aunque el username es `postgres.bmulgijtddwdwaajktay` — cómo lo soluciono?"
- **Prompt 2:** "puede ser que Spring Boot no sale porque tengo Dockerfiles que no uso en el backend?"
- **Prompt 3:** "crea un `LocalDataSourceConfig.java` con `@Profile("local") @Primary` que bypasee todas las variables de entorno y use las credenciales hardcodeadas para que no haya ambigüedad"
- **Prompt 4:** "esta es la nueva password `19VX5FTtdniXvkho` — actualiza todos los ficheros"
- **Prompt 5:** "analízame el flujo de los componentes de la interfaz y cómo se comunican entre capas. Actualízame los documentos y la skill Votify que tenía la comunicación frontend→Supabase"

---

### 3) Salidas relevantes (resumen corto)

- **Prompt 1 →** La IA diagnosticó que el mensaje `user "postgres"` de pgBouncer NO indica el username enviado, sino el usuario interno del pooler — esto enmascaraba el error real (contraseña incorrecta/caducada). Propuso pasos de depuración: verificar el perfil Spring activo, crear DataSource programático.
- **Prompt 2 →** La IA confirmó que los Dockerfiles no impiden el arranque de Spring Boot. El problema era la autenticación, no la configuración Docker.
- **Prompt 3 →** La IA generó `LocalDataSourceConfig.java` con `@Configuration @Profile("local") @AutoConfigureBefore(DataSourceAutoConfiguration.class) @Primary`. También añadió un logger `>>> LocalDataSourceConfig ACTIVO` para confirmar la activación. Esto permitió confirmar que el bean se cargaba y el username era correcto, descartando la configuración como causa.
- **Prompt 4 →** La IA actualizó la contraseña en `LocalDataSourceConfig.java`, `application-local.properties`, `application.properties` y `.env`. El backend arrancó correctamente con la nueva credencial.
- **Prompt 5 →** La IA analizó el flujo de capas: `Página TSX → client.ts → axios.ts (baseURL=/api) → proxy Vite → Spring Boot :8080 → PostgreSQL Supabase`. Creó CLAUDE.md en ambos repos, ADR-005, actualizó ADR-001 y BACKEND-RESUMEN.md, y reescribió la SKILL.md del proyecto eliminando todas las referencias al acceso directo a Supabase.

---

### 4) Qué aceptamos y qué rechazamos

- **Aceptado:** `LocalDataSourceConfig.java` con credenciales hardcodeadas en perfil `local` → por qué: garantiza que en local NUNCA hay ambigüedad entre variables de entorno del SO, perfiles Spring y properties. El fichero está en `.gitignore` implícitamente (o se añade) y en producción el perfil `local` no se activa.
- **Aceptado:** `mvn clean spring-boot:run` en lugar de `mvn spring-boot:run` → por qué: Maven incremental no detecta nuevos ficheros `.java` sin `clean`. Documentado en CLAUDE.md.
- **Aceptado:** Diagnóstico de WSL vs Windows PowerShell → el frontend en WSL no puede llegar al backend en Windows por `localhost:8080` (entornos de red distintos). Solución: ejecutar ambos desde WSL. Documentado en `CLAUDE.md` del frontend.
- **Rechazado (no aplicable):** La IA sugirió inicialmente que los Dockerfiles podían interferir — esto se descartó porque Docker no estaba corriendo y Spring Boot no los lee en tiempo de ejecución.

---

### 5) Cómo lo verificamos

- [X] Backend arranca con `mvn clean spring-boot:run` y el log muestra `>>> LocalDataSourceConfig ACTIVO - conectando como: postgres.bmulgijtddwdwaajktao`.
- [X] Log de Spring Boot muestra `The following 1 profile is active: "local"`.
- [X] Hibernrte imprime las queries SQL al arrancar (`ddl-auto=update` sin errores de conexión).
- [X] `GET http://localhost:8080/actuator/health` devuelve `{"status":"UP"}`.
- [X] Frontend en WSL: `ECONNREFUSED` se resuelve ejecutando el backend también desde WSL.
- [X] 98 tests pasan (`mvn test`) tras los cambios de Sprint 1 en el backend.

---

### 6) Resultado final / decisión humana

La causa del fallo de autenticación fue una **contraseña caducada/cambiada en Supabase**, no un error de configuración. El mensaje de pgBouncer (`user "postgres"`) enmascaró esto durante varios intentos. La creación de `LocalDataSourceConfig.java` fue útil como herramienta de diagnóstico (confirmó que el username era correcto) y se mantiene como solución robusta para el perfil local.

La documentación del proyecto se actualizó completamente para reflejar la arquitectura Sprint 1:
- `CLAUDE.md` en backend y frontend: contexto completo para Claude Code
- `ADR-005`: migración frontend Supabase → Spring Boot REST
- `BACKEND-RESUMEN.md`: actualizado con Sprint 1 (nuevos endpoints, 98 tests)
- `SKILL.md` del proyecto: eliminadas todas las referencias a acceso directo a Supabase

Total de tests al cierre del Sprint 1: **98 tests** (73 unitarios + 25 integración), todos en verde.

Referencias en el repo:
- ADR-005: `docs/adr/ADR-005-migracion-frontend-supabase-a-backend.md`
- Config: `src/main/java/com/votify/config/LocalDataSourceConfig.java`
- Frontend: `src/api/client.ts` (330 líneas, 48+ funciones), `src/api/axios.ts`
