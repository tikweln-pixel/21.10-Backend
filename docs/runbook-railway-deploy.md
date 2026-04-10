# Runbook: Despliegue del backend Votify en Railway

## Problema de partida
El repo original del backend pertenecía a otro colaborador sin acceso para
desplegar. Se creó un repo propio en GitHub para tener control total.

---

## Pasos realizados

### 1. Crear repo propio en GitHub
1. GitHub → **New repository**
   - Nombre: `21.03_votify-backend`
   - Privado, sin README ni .gitignore
2. Añadir el nuevo repo como remote y subir el código:
   ```bash
   cd 21.03-Backend
   git remote add mirepo https://github.com/Nmsevinf/21.03_votify-backend.git
   git update-index --chmod=+x mvnw   # dar permisos de ejecución al wrapper Maven
   git add .
   git commit -m "fix: mvnw executable permission"
   git push mirepo main
   ```
   > ⚠️ El permiso de `mvnw` es obligatorio — sin él Railway falla el build.

### 2. Desplegar en Railway
1. railway.app → **New Project** → **Deploy from GitHub repo**
2. Seleccionar `Nmsevinf/21.03_votify-backend`
3. Railway detecta Maven/Java automáticamente (Railpack)
4. Añadir variable de entorno en **Variables**:
   - `SPRING_PROFILES_ACTIVE` = `production`
5. Esperar a que el deploy termine — verificar en **Deployments** que aparece verde

### 3. Conectar el frontend con el backend
1. Vercel → proyecto `votify-frontend` → **Settings** → **Environment Variables**
2. Añadir:
   - `VITE_API_URL` = `https://votify-backend-production-52a5.up.railway.app/api`
3. Forzar redeploy:
   ```bash
   cd votify-frontend
   git commit --allow-empty -m "redeploy: apuntar a Railway backend"
   git push
   ```

---

## Problemas encontrados y soluciones

### Error: `./mvnw: Permission denied`
**Causa:** el archivo `mvnw` no tenía permisos de ejecución en el repo.  
**Solución:**
```bash
git update-index --chmod=+x mvnw
git commit -m "fix: mvnw executable permission"
git push mirepo main
```

### Error: push rechazado tras renombrar el repo
**Causa:** el remote `mirepo` seguía apuntando al nombre antiguo.  
**Solución:**
```bash
git remote set-url mirepo https://github.com/Nmsevinf/21.03_votify-backend.git
git push mirepo main
```

### Error: CORS bloqueado desde Vercel (403 Forbidden)
**Causa:** había dos clases de configuración CORS (`CorsConfig.java` y
`WebConfig.java`) que se contradecían. `WebConfig` solo permitía `localhost`
y bloqueaba cualquier petición desde Vercel.  
**Solución:** vaciar `WebConfig.java` dejando solo la configuración en
`CorsConfig.java`, que incluye:
```java
.allowedOriginPatterns(
    "http://localhost:*",
    "https://*.vercel.app",
    "https://votify-frontend-nine.vercel.app"
)
```
Luego hacer push:
```bash
git add src/main/java/com/votify/config/WebConfig.java
git commit -m "fix: eliminar WebConfig CORS duplicado que bloqueaba Vercel"
git push mirepo main
```

---

## Comandos de mantenimiento

### Publicar cambios del backend a Railway
```bash
cd 21.03-Backend
git push mirepo main
```

### Para colaboradores — añadir el remote de Nina
```bash
git remote add nina https://github.com/Nmsevinf/21.03_votify-backend.git
git push nina main
```

### Verificar que el backend responde
Abrir en el navegador:
```
https://votify-backend-production-52a5.up.railway.app/actuator/health
https://votify-backend-production-52a5.up.railway.app/api/events
```

---

## URLs de producción
| Servicio | URL |
|---|---|
| Frontend | `https://votify-frontend-nine.vercel.app` |
| Backend | `https://votify-backend-production-52a5.up.railway.app` |
| Health check | `https://votify-backend-production-52a5.up.railway.app/actuator/health` |
