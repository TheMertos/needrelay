# NeedRelay

Connecting urgent needs with available resources.

**License:** [MIT](LICENSE)

## Screenshots

Add images under [`docs/screenshots/`](docs/screenshots/) and embed them here, for example:

```markdown
![Public discovery map](docs/screenshots/public-map.png)
![Organizer inbox](docs/screenshots/organizer-inbox.png)
```

## Structure

| Folder | Role |
|--------|------|
| `needrelay-backend/` | Spring Boot API (JWT, Liquibase, OpenAPI) |
| `needrelay-frontend/` | React + Vite + Mantine (Orval client) |
| `needrelay-db/` | PostgreSQL Docker stack (local) |
| `deploy/` | Emergency production stack (Caddy + app image + Postgres, host network) |
| `scripts/` | Helper scripts (e.g. release) |

## Prerequisites

- Java 25+, Maven Wrapper
- Node.js 22+
- Docker

## Emergency production deploy

For a fast VPS bring-up (UFW ports **22 / 80 / 443** only, host networking, `restart: always`), see **[`deploy/DEPLOY.md`](deploy/DEPLOY.md)**.

Summary:

1. Point DNS at the server.
2. Configure UFW (22, 80, 443).
3. Copy `deploy/.env.example` → `deploy/.env` and set `DOMAIN` + secrets.
4. `cd deploy && docker compose pull && docker compose up -d`

Image: [`themertos/needrelay`](https://hub.docker.com/r/themertos/needrelay) (published by GitHub Actions).

## Local development

### Start infrastructure

```bash
cd needrelay-db && docker compose up -d
```

Or all-in-one app + Postgres (build locally):

```bash
docker compose up -d --build
```

Or run API/UI on the host:

```bash
# DB
cd needrelay-db && docker compose up -d

# API (http://localhost:8080, Swagger at /swagger-ui.html)
cd needrelay-backend
cp .env.example .env   # optional
./mvnw spring-boot:run

# UI (http://localhost:5173)
cd needrelay-frontend
cp .env.example .env
yarn install
yarn generate:api   # from openapi/openapi.yaml
yarn dev
```

Frontend tests:

```bash
cd needrelay-frontend
yarn test          # Vitest unit tests
yarn test:e2e      # Playwright (optional E2E_PUBLIC_SLUG for public page layout)
```

Bootstrap admin (created on first start if DB is empty):

- Email: `admin@needrelay.local`
- Password: `ChangeMeAdmin123!`

Invite-only registration: create invites in the UI (`/invites`) or via `POST /api/invites`, then open `/register?invite=<token>`.
If the invite includes an email, Resend sends the invite link (requires `RESEND_API_KEY`).

## Email (Resend)

Set in `needrelay-backend/.env`:

- `RESEND_API_KEY` — Resend API key
- `RESEND_FROM_EMAIL` — verified sender (default Resend onboarding address for tests)
- `APP_PUBLIC_BASE_URL` — frontend origin used in email links (e.g. `http://localhost:5173`)

Without `RESEND_API_KEY`, invite/reset emails are skipped (logged), but password-reset tokens are still created.

## Account management

- `/account` — update display name, change password
- `/forgot-password` + `/reset-password` — password reset via email
- `/admin/organizers` — ADMIN only: ban / unban organizers

## OpenAPI → frontend client

1. Keep `needrelay-frontend/openapi/openapi.yaml` in sync with the backend (or export from `/v3/api-docs`).
2. Regenerate: `yarn generate:api` in `needrelay-frontend`.

## Tests

```bash
cd needrelay-backend && ./mvnw test
```

Uses in-memory H2 (PostgreSQL mode). Production schema is applied by Liquibase against PostgreSQL.

## Releasing

Publish the next patch version (auto bump from the latest `v*` tag), commit, tag, and push:

```powershell
.\scripts\release.ps1
```

GitHub Actions builds the image and pushes `themertos/needrelay` (`latest` on `main`, version tags on `v*`). Configure repository secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`.

## License

This project is licensed under the [MIT License](LICENSE).
