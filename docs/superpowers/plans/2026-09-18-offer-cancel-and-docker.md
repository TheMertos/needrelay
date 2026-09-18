# Offer cancel + All-in-one Docker — Implementation Plan

> **For agentic workers:** Execute task-by-task. Spec: `docs/superpowers/specs/2026-09-18-offer-cancel-design.md`

**Goal:** (1) Organizer cancel offers → `CANCELLED`; (2) All-in-one app Docker image with SPA static + Actuator liveness/readiness (DB), Compose with separate Postgres.

**Architecture:** Part 1 follows existing coming/received endpoints. Part 2 multi-stage Dockerfile copies Vite `dist` into Spring `static`, SPA fallback controller, Actuator health groups.

**Tech Stack:** Spring Boot 4, Liquibase (enum as varchar — no migration required for CANCELLED), Orval, Docker Compose, Actuator

## Global Constraints

- English code/comments; no commits unless asked.
- Part 1 before Part 2 verification gates independently.

---

### Part 1 — Offer cancel

1. Add `CANCELLED` to `OfferStatus`; `cancelOffer` in `OfferService`; reject cancel for RECEIVED/CANCELLED; reject edit/coming/receive for CANCELLED.
2. `POST .../offers/{offerId}/cancel` on `ReliefRequestController`.
3. OpenAPI + Orval regenerate.
4. OfferInbox + ManageRequestPage cancel; i18n all locales.
5. OfferServiceIT cancel tests; backend tests + frontend build.

### Part 2 — All-in-one Docker

1. Add `spring-boot-starter-actuator`; configure liveness/readiness + DB; Security permit paths.
2. SPA static resource + fallback (not for `/api`, `/actuator`).
3. Root `Dockerfile` multi-stage; root or `deploy/docker-compose.yml` postgres+app.
4. Smoke: health endpoints; document run commands.

No commits unless user asks.
