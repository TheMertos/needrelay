# All-in-one Docker (UI + API)

Build and run from the **repository root**:

```bash
docker compose -f docker-compose.app.yml up -d --build
```

- App: http://localhost:8080 (SPA + `/api`)
- Health: http://localhost:8080/actuator/health/liveness  
  Readiness (includes DB): http://localhost:8080/actuator/health/readiness
- Postgres: localhost:5432 (same credentials as `needrelay-db`)

Optional env overrides: `JWT_SECRET`, `ADMIN_*`, `CORS_ALLOWED_ORIGINS`, `APP_PUBLIC_BASE_URL`, `RESEND_*`.

Local Vite+API split workflow is unchanged (`needrelay-frontend` / `needrelay-backend` Dockerfiles remain for that).
