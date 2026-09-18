# Dev DB seed script (Yarn + wipe)

**Date:** 2026-09-18  
**Status:** Approved for implementation (approach 1: Node + `pg`)

## Goal

Manual, on-demand **wipe + seed** of the local Postgres database so developers can fill realistic data for discovery, public pages, login, offers, and contacts. Package manager: **Yarn 4** only.

## Decisions

| Topic | Choice |
| --- | --- |
| Trigger | Manual command only (no backend auto-seed) |
| Scope | Broad seed (ACTIVE + ARCHIVED, open/partial/covered/closed needs, offer, contacts) |
| Re-run | Wipe then seed |
| Mechanism | Node script via `pg` + BCrypt hashes (`bcryptjs`) |
| Package manager | Yarn 4 (`nodeLinker: node-modules`) |

## Location & run

- Directory: `needrelay-db/seed/`
- Files: `package.json`, `.yarnrc.yml`, `seed.mjs`, `README.md`
- Commands: `yarn install` then `yarn seed`
- DB defaults: `localhost:5432`, db/user/password `needrelay` (override with `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD`)
- Postgres must already be running (`needrelay-db/docker-compose.yml`)

## Wipe order

Delete/truncate (Liquibase changelog tables untouched):

1. `offers`
2. `relief_request_comments`
3. `needs`
4. `relief_requests`
5. `organizer_contacts`
6. `refresh_tokens`
7. `password_reset_tokens`
8. `invites`
9. `organizers`

## Seed content

Fixed UUIDs/slugs for stable demos:

- **ADMIN** `admin@needrelay.local` / `Admin123!` (role ADMIN, with description)
- **ORGANIZER** `org@needrelay.local` / `Org1234!` (role ORGANIZER, description + 2 contacts)
- **2 ACTIVE** relief requests (distinct lat/lng + location labels, e.g. Aleppo / Damascus) with OPEN + PARTIALLY_COVERED needs
- **1 ARCHIVED** request with an OPEN need (must not appear in public discovery)
- On ACTIVE: one COVERED and one CLOSED need (excluded from discovery)
- **1 Offer** against an OPEN need

Print login emails/passwords and public slugs when finished.

## Out of scope

- Auto-seed on application startup
- Production use
- Editing seed via JSON/YAML config file (v1 = hardcoded dataset in `seed.mjs`)
- npm / package-lock

## Safety

- Dev-only; README warns that wipe destroys all app data in the target DB
- Do not commit real production secrets; seed passwords are documented demo credentials only
