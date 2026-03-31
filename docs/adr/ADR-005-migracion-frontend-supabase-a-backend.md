# ADR-005: Migración del frontend de acceso directo a Supabase a API REST Spring Boot

**Fecha:** 31-03-2026
**Sprint:** S1
**Estado:** Completado

---

### 1) Contexto

El frontend de Votify (`votify-frontend`) fue construido inicialmente accediendo directamente a Supabase mediante el cliente JavaScript `@supabase/supabase-js`. Este cliente hacía 48+ llamadas directas a la base de datos desde el navegador, contenidas en un único archivo `src/api/client.ts` de ~671 líneas. La arquitectura original era:

```
Frontend React → @supabase/supabase-js → PostgreSQL Supabase
```

En el Sprint 0 se construyó el backend Spring Boot (ADR-001), pero el frontend seguía sin usarlo. En el Sprint 1, se completó la migración para que **toda petición de datos pase por el backend REST**, eliminando la dependencia directa del frontend con Supabase.

La arquitectura final es:

```
Frontend React → Axios (/api/*) → Backend Spring Boot (:8080) → PostgreSQL Supabase
```

---

### 2) Opciones consideradas

- **Opción A:** Mantener acceso directo desde frontend a Supabase JS client en paralelo con el backend (coexistencia).
- **Opción B:** Migrar completamente — todo pasa por el backend, eliminar `@supabase/supabase-js` del frontend.
- **Opción C:** Migración parcial — solo los endpoints que el backend ya tiene; mantener Supabase para el resto.

---

### 3) Criterios de decisión

- Requisito académico: el backend debe ser la única capa de acceso a datos.
- Seguridad: las credenciales de Supabase no deben estar en el cliente (browser).
- Mantenibilidad: una sola fuente de verdad para la lógica de negocio.
- Compatibilidad: las firmas de las funciones exportadas deben mantenerse para no romper las 10+ páginas.

---

### 4) Decisión tomada

Se elige la **Opción B** (migración completa). El frontend ahora:

1. Usa `axios` (instancia en `src/api/axios.ts`) con `baseURL=/api`
2. El proxy de Vite redirige `/api` → `http://localhost:8080` en desarrollo
3. `src/api/client.ts` reescrito (~330 líneas) con las mismas funciones exportadas pero llamando al backend
4. `src/api/supabase.ts` eliminado
5. `@supabase/supabase-js` desinstalado

Para completar la migración, se añadieron **12 nuevos endpoints/funcionalidades al backend** (Sprint 1):

| # | Endpoint/Cambio | Descripción |
|---|-----------------|-------------|
| 1 | `VotingDto.manuallyModified` | Campo nuevo para auditoría de intervención manual |
| 2 | `EventDto.categories` (antes `categoryNames`) | Renombrado para coherencia con frontend |
| 3 | `EventParticipationDto.email` | Alias JSON para campo `userEmail` |
| 4 | `DELETE /api/events/{id}` (cascade) | Borra votings, participaciones, comentarios en cascada |
| 5 | `DELETE /api/categories/{id}` (cascade) | Cascade delete completo |
| 6 | `GET /api/projects/{id}/comments` | Comentarios de un proyecto |
| 7 | `GET /api/competitors/{userId}/comments` | Comentarios recibidos por competidor |
| 8 | `GET /api/categories/{id}/active-voters` | Votantes que han votado en la categoría |
| 9 | `GET /api/votings/by-competitors?ids=...` | Votos filtrados por lista de competidores |
| 10 | `GET /api/votings/by-voter-competitor` | Votos de un voter+competitor específico |
| 11 | `GET /api/projects/{id}/competitors` | IDs de competidores asignados a proyecto |
| 12 | `POST /api/events/{id}/competitors/register` y `.../voters/register` | Registro completo con creación de User+Participant en una transacción |

---

### 5) Consecuencias

- **Positivas:**
  - Las credenciales de Supabase (URL, contraseña) ya no están expuestas en el bundle del cliente.
  - Toda la lógica de negocio (validaciones, permisos, transformaciones) centralizada en el backend.
  - El frontend es más ligero: ~341 KB menos por eliminar `@supabase/supabase-js`.
  - Una sola API tipada (`client.ts`) para 10 páginas sin cambios en las páginas.

- **Negativas / trade-offs:**
  - El frontend requiere que el backend esté corriendo para funcionar (no hay modo offline).
  - Funciones de Supabase Auth y Realtime no están disponibles a través del backend (fuera del alcance del MVP).
  - Latencia adicional: cada petición pasa por una capa extra (frontend → backend → BD).

- **Riesgos y mitigaciones:**
  - Riesgo: el proxy Vite solo funciona si backend y frontend corren en el mismo entorno de red (ambos en WSL o ambos en Windows). Mitigación: documentado en `CLAUDE.md` del frontend.
  - Riesgo: el backend en producción puede tener CORS diferente al desarrollo. Mitigación: `WebConfig.java` y `CorsConfig.java` permiten todos los orígenes; en producción restringir a dominio del frontend.

---

### 6) Evidencia

- `src/api/axios.ts`: instancia axios con baseURL=/api e interceptor res.data.
- `src/api/client.ts`: 330 líneas, 48+ funciones, cero imports de Supabase.
- `vite.config.ts`: proxy `/api` → `http://localhost:8080`.
- `package.json`: sin `@supabase/supabase-js` en dependencies.
- Backend: `LocalDataSourceConfig.java`, nuevos repos y servicios en `VotingRepository`, `EventParticipationRepository`, `CommentRepository`.
- Commits Sprint 1: migración completa frontend + endpoints backend.
