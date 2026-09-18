# Offer cancel + All-in-one Docker app

**Date:** 2026-09-18  
**Status:** Approved for implementation (both parts)

This document covers two deliverables that were decided in the same session.

---

# Part 1 — Organizer offer cancel (`CANCELLED`)

## Goal

Organizers can mark help offers as cancelled when the provider cannot deliver (e.g. reported by phone while PENDING or on the way / COMING). Cancelled offers stay visible in the organizer inbox with a badge and no longer count toward pending quantities.

## Locked decisions

| Topic | Choice |
| --- | --- |
| Who cancels | **Organizer only** (provider informs by phone) |
| Allowed from | **PENDING** and **COMING** only (not RECEIVED) |
| Persistence | New status **`CANCELLED`** (not hard delete) |
| Inbox | Cancelled offers **remain visible** with badge |
| API style | Dedicated `POST /api/offers/{id}/cancel` |

## Domain

`OfferStatus`: `PENDING | COMING | RECEIVED | CANCELLED`

- Create still starts as `PENDING`.
- Coming / received rules unchanged.
- Cancel: if status is PENDING or COMING → set `CANCELLED`; otherwise `409`/`400` with a clear error code (e.g. `OFFER_NOT_CANCELLABLE`).
- `quantityPending` on needs: sum of expected quantity for **PENDING + COMING only** (CANCELLED excluded — already true if `PENDING_STATUSES` stays as today).
- Existing delete endpoint: keep as-is for now (organizer may still delete rows); cancel is the preferred lifecycle end for undelivered help.

## API

- `POST /api/offers/{id}/cancel` (authenticated, owner of the relief request)
- Response: `OfferResponse` with `status: CANCELLED`
- OpenAPI update → regenerate Orval client

## Frontend

- `OfferInbox`: “Cancel” action when status is PENDING or COMING
- Badge for `CANCELLED`; hide coming / received / edit actions for cancelled (delete may remain)
- i18n: `offer.cancel`, `offer.status.CANCELLED` in all locale files (prefer all 20 for consistency)

## Tests (Part 1)

- Backend IT: cancel PENDING → CANCELLED; cancel COMING → CANCELLED; cancel RECEIVED → error; ownership check
- Frontend: OfferInbox wired to API; e2e smoke optional

## Non-goals (Part 1)

- Public self-service cancel link/token
- Cancelling RECEIVED (undo received quantity)

---

# Part 2 — All-in-one Docker app (UI + API)

## Goal

Ship a single container image that serves the Vite SPA as static assets from Spring Boot and exposes the API on the same origin/port. Postgres remains a separate image. Health checks support Compose orchestration.

## Locked decisions

| Topic | Choice |
| --- | --- |
| App packaging | **All-in-one**: UI + API in one container |
| Database | **Postgres stays its own image** |
| Health | **Liveness + readiness**; readiness includes DB reachability |
| Frontend API base | Same-origin (`VITE_API_URL=""` at image build) |

## Build

Multi-stage Dockerfile (repo root recommended):

1. **Frontend stage:** Node/Yarn build Vite → `dist/`
2. **Backend stage:** Copy `dist/` into `src/main/resources/static/` (or Maven resource overlay), `mvn -DskipTests package`
3. **Runtime:** Eclipse Temurin JRE + fat JAR; expose `8080`

Keep existing separate frontend/backend Dockerfiles for local/dev if useful; production path is the all-in-one image.

## Spring Boot SPA serving

- Static files from classpath `/static`
- SPA fallback: non-API, non-actuator routes → `index.html` (React Router)
- Do not intercept `/api/**` or `/actuator/**`

## Actuator / health

- Enable Spring Boot Actuator health groups:
  - **Liveness:** `/actuator/health/liveness` (process up)
  - **Readiness:** `/actuator/health/readiness` (includes DB)
- Security: permit health endpoints only (already partially allowlisted; extend to liveness/readiness paths as needed)
- Do not expose full actuator management surface publicly

## Compose

- Services: `postgres` (existing healthcheck) + `app` (all-in-one image)
- `app` `depends_on` postgres with `condition: service_healthy`
- `app` Docker/`compose` healthcheck hitting readiness (e.g. `curl -f http://localhost:8080/actuator/health/readiness`)
- Env: JDBC URL to `postgres` service, JWT secrets, etc. via `.env` / compose env

## Tests (Part 2)

- Image builds successfully (CI or local smoke)
- After compose up: readiness becomes healthy once DB is up
- `GET /` returns SPA; `GET /api/...` still API

## Non-goals (Part 2)

- Postgres inside the app container
- Replacing local Vite/Maven dev workflow
- Full Kubernetes manifests (Compose first; k8s later if needed)
