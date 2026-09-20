# NeedRelay

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Docker Hub](https://img.shields.io/badge/docker-themertos%2Fneedrelay-2496ED?logo=docker&logoColor=white)](https://hub.docker.com/r/themertos/needrelay)

NeedRelay is an open-source web application for **public authorities, municipalities, relief organizations, and local coordination teams** responding to natural disasters and other emergencies: floods, earthquakes, storms, wildfires, displacement, and similar events where people need water, shelter, food, clothing, or other urgent supplies — and helpers need one place to see what is still missing.

It is meant to be **stood up when a crisis starts**, not run as a long-lived SaaS. A volunteer with a Linux VPS and a domain can go live from [`deploy/`](deploy/) in minutes, without building the app.

NeedRelay does not replace official emergency services, emergency dispatch, or institutional verification. It is a coordination board for **needs and offers** once people are already organizing a response.

The project is currently an **early release for piloting and feedback**. It is intended to be deployed and operated by an organization that understands the local response context and can verify its participating organizers.

## What it does

**Organizations** (public authorities, municipalities, relief groups, and local coordinators) publish **relief requests**: a location, a public page, and a list of needs with quantities. Organizer accounts are invite-only, so the public cannot freely create or manage requests.

**The public** opens a map and list of active help points, searches by place or need, and submits offers (how much they can provide, as a person or an organization). The responsible organizers review and manage those offers and contact helpers about delivery or coordination. Each public page shows the contact information configured by the organizer.

Typical flow:

1. An organizer creates a relief request for a camp, hub, or affected area.
2. They add needs (for example water tanks, tarps, food) with required amounts.
3. Helpers find the request, submit offers, and organizers track what is pending, received, or still open.

Optional email (invites and password reset) uses Resend when `RESEND_API_KEY` is set.

## Access and responsibility model

- Organizer accounts are created through invitations; the operator decides which institutions and people receive them.
- Invitation-only access is an access-control mechanism, not proof that an organization is genuine. The deploying authority is responsible for verifying its organizers.
- Helpers can submit offers without managing requests. Organizers remain responsible for reviewing offers, confirming delivery details, and updating need statuses.
- NeedRelay does not process donations or payments and does not guarantee that an offer, delivery, or organization is genuine.
- Do not use it as a replacement for emergency services. In a life-threatening emergency, contact the relevant official emergency service.

## Go live now

**Follow [`deploy/DEPLOY.md`](deploy/DEPLOY.md).** You need a Linux VPS, Docker Compose, and a DNS name. Copy `deploy/.env.example` to `deploy/.env`, set the domain and secrets, then:

```bash
cd deploy
docker compose pull
docker compose up -d
```

The published image is [`themertos/needrelay`](https://hub.docker.com/r/themertos/needrelay).

## Try it on your machine

Published image + Postgres (see [`local-test/README.md`](local-test/README.md)):

```bash
cd local-test
docker compose pull
docker compose up -d
```

Open [http://localhost:8081](http://localhost:8081). Empty database: `admin@needrelay.local` / `ChangeMeAdmin123!`. Optional demo seed:

```bash
docker compose --profile seed run --rm seed
```

After seed: admin `Admin123!`, organizer `org@needrelay.local` / `Org1234!`.

Build from this repo instead:

```bash
docker compose up -d --build
```

## Project layout

| Path | Role |
|------|------|
| `deploy/` | Emergency production stack (Caddy, app image, Postgres) |
| `local-test/` | Published image + Postgres; optional demo seed |
| `needrelay-backend/` | Spring Boot API |
| `needrelay-frontend/` | React UI |
| `needrelay-db/` | Local PostgreSQL for development |

Development (API on `:8080`, UI on `:5173`):

```bash
cd needrelay-db && docker compose up -d
cd needrelay-backend && ./mvnw spring-boot:run
cd needrelay-frontend && yarn install && yarn dev
```

Tests:

```bash
cd needrelay-backend && ./mvnw test
cd needrelay-frontend && yarn test
```

## Releases

`.\scripts\release.ps1` stages everything, commits (`Release vX.Y.Z` if there are changes), pushes the branch, then tags **latest** and the next **vX.Y.Z** and pushes those tags. GitHub Actions publishes `themertos/needrelay:latest` from `main`, and `vX.Y.Z` from version tags. Repository secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`.

## License

[MIT](LICENSE)
