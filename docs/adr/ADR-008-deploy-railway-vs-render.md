# ADR-008: Plataforma de despliegue del backend — Railway vs Render

**Fecha:** 09-04-2026
**Sprint:** S1
**Estado:** Aprobado

---

### 1) Contexto

El backend Spring Boot necesita un servidor en la nube para que el frontend
desplegado en Vercel pueda consumir la API REST en producción. Se evaluaron dos
opciones de PaaS (Platform as a Service) con plan gratuito: Render y Railway.

El contexto adicional es que el repo original del backend pertenece a otro
colaborador del equipo que no podía dar permisos de despliegue. Se creó un
repo propio (`Nmsevinf/21.03_votify-backend`) como fork funcional para poder
desplegar de forma autónoma.

---

### 2) Opciones consideradas

- **Opción A — Render (nativo Java):** Render no soporta Java como runtime
  nativo. Solo soporta Node, Python, Ruby, Go, Rust y Elixir. Para Java
  requiere Dockerfile.
- **Opción B — Render (Docker):** Render con Dockerfile. Requiere crear y
  mantener un Dockerfile, añadiendo complejidad innecesaria para el MVP.
- **Opción C — Railway (elegida):** Railway detecta automáticamente proyectos
  Maven/Spring Boot y los compila sin configuración adicional. Solo requiere
  que `mvnw` tenga permisos de ejecución.

---

### 3) Criterios de decisión

- Sin necesidad de Dockerfile.
- Plan gratuito suficiente para el MVP académico.
- Detección automática del stack Java/Maven.
- Deploy automático en cada push a `main`.
- Facilidad de configuración de variables de entorno.

---

### 4) Decisión tomada

**Railway (Opción C).**

Pasos realizados:
1. Se intentó Render primero — falló porque no soporta Java nativo.
2. Se migró a Railway: New Project → Deploy from GitHub repo →
   `Nmsevinf/21.03_votify-backend`.
3. Fix necesario: `git update-index --chmod=+x mvnw` para dar permisos de
   ejecución al wrapper de Maven.
4. Variables de entorno configuradas en Railway dashboard:
   `SPRING_PROFILES_ACTIVE=production`
5. El frontend en Vercel apunta al backend via `VITE_API_URL`:
   `https://votify-backend-production-15c8.up.railway.app/api`

---

### 5) Flujo de despliegue resultante

```
Push a Nmsevinf/21.03_votify-backend (main)
        ↓ trigger automático
Railway build: ./mvnw clean package -DskipTests
        ↓
Railway deploy: java -jar target/votify-backend-0.0.1-SNAPSHOT.jar
        ↓
URL pública: https://votify-backend-production-15c8.up.railway.app
        ↑
Vercel (frontend) → VITE_API_URL → /api/*
```

---

### 6) Consecuencias

- **Positivas:**
  - Zero configuración extra — Railway detecta Maven automáticamente.
  - Deploy automático en cada push.
  - Los colaboradores solo necesitan hacer push a `Nmsevinf/21.03_votify-backend`
    para que los cambios lleguen a producción.

- **Negativas / trade-offs:**
  - Plan gratuito de Railway tiene límite de horas de ejecución mensuales
    (~500h). Suficiente para uso académico.
  - El repo de despliegue (`Nmsevinf`) es distinto al repo de desarrollo
    original del equipo. Los colaboradores deben añadir el remote de Nina
    para publicar cambios.

---

### 7) Evidencia

- Commit: `fix: mvnw executable permission` — necesario para que Railway
  pudiera ejecutar el build.
- URL backend producción: `https://votify-backend-production-15c8.up.railway.app`
- Variable Vercel: `VITE_API_URL=https://votify-backend-production-15c8.up.railway.app/api`
