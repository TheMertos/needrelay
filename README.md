# NeedRelay

Coordinate urgent needs with people and organizations who can help. Organizers publish requests; the public can discover them and offer support.

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Docker Hub](https://img.shields.io/badge/docker-themertos%2Fneedrelay-2496ED?logo=docker&logoColor=white)](https://hub.docker.com/r/themertos/needrelay)

## Quick start

```bash
docker compose up -d --build
```

Open [http://localhost:8080](http://localhost:8080). Bootstrap admin (empty database only): `admin@needrelay.local` / `ChangeMeAdmin123!`

Change the admin password immediately.

## Production

See [`deploy/DEPLOY.md`](deploy/DEPLOY.md). The published image is [`themertos/needrelay`](https://hub.docker.com/r/themertos/needrelay).

## Local development

| Path | Role |
|------|------|
| `needrelay-backend/` | Spring Boot 4 API (JWT, Liquibase, OpenAPI) |
| `needrelay-frontend/` | React, Vite, Mantine |
| `needrelay-db/` | Local PostgreSQL |
| `deploy/` | Production Compose (Caddy, app image, Postgres) |

```bash
cd needrelay-db && docker compose up -d

cd needrelay-backend && ./mvnw spring-boot:run   # http://localhost:8080
cd needrelay-frontend && yarn install && yarn dev  # http://localhost:5173
```

Registration is invite-only (`/invites`, then `/register?invite=<token>`). Optional email (invites and password reset) uses `RESEND_API_KEY` and `APP_PUBLIC_BASE_URL`.

Regenerate the TypeScript API client after OpenAPI changes: `yarn generate:api` in `needrelay-frontend`.

```bash
cd needrelay-backend && ./mvnw test
cd needrelay-frontend && yarn test
```

## Releases

`.\scripts\release.ps1` creates the next `vMAJOR.MINOR.PATCH` git tag (first tag: `v0.0.1`) and pushes it. GitHub Actions publishes:

- `themertos/needrelay:latest` on `main`
- `themertos/needrelay:vX.Y.Z` and `vX.Y` on version tags

Repository secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` (Docker Hub access token).

## License

[MIT](LICENSE)
