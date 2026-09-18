# Public help discovery on `/`

**Date:** 2026-09-18  
**Status:** Approved for implementation (approach 1)

## Goal

Replace the marketing landing on `/` with a **public help discovery** surface: map of help points (ACTIVE relief requests) and a list of open needs. App header stays (login, language, etc.). Old landing hero/CTA content is removed (not moved to `/about`).

## Decisions

| Topic | Choice |
| --- | --- |
| Route | `/` replaces landing |
| Map | Pins = ACTIVE relief requests (hilfepunkte) |
| List | Needs from those requests |
| Header | Unchanged (login, language, …) |
| Location filter | Map pin click filters needs; plus free-text search |
| Need click | Drawer/panel with short info + link to `/r/:slug` |
| Landing content | Gone |
| API approach | Single `GET /api/public/discovery`; client-side filter |
| Need statuses in list | `OPEN` + `PARTIALLY_COVERED` only |

## API

### `GET /api/public/discovery` (permitAll)

Returns one payload for map + list. No pagination in v1.

**`points[]`** (ACTIVE relief requests):

- `id`, `title`, `locationLabel`, `latitude`, `longitude`, `publicSlug`

**`needs[]`** (needs belonging to those requests, status `OPEN` or `PARTIALLY_COVERED`):

- `id`, `title`, `priority`, `status`, `requestId`, `publicSlug`, `locationLabel`, `organizationName`

Existing `GET /api/public/relief-requests/{slug}` stays for the full public request page.

No entity types in responses; OpenAPI + Orval regen after DTO change.

## UI

- **Page:** Discovery replaces `LandingPage` on the index route.
- **Layout:** Map-first on mobile; desktop map + list side-by-side (or map top / list below consistent with public map-top pattern).
- **Search:** Free-text field above the list; filters `locationLabel` and need `title` (client-side).
- **Pin click:** Selects that request; list shows only its needs. Clear / “All” resets selection.
- **Map:** New multi-marker component (e.g. `DiscoveryMap`); existing `LocationMap` stays single-pin for create/manage/public detail.
- **Drawer:** Need title, priority, location, organization name + link “open help page” → `/r/:slug`.
- **i18n:** New discovery keys; remove or stop using landing marketing copy.
- **Empty / error:** Empty state when no ACTIVE points; inline error + retry on API failure; empty list message when filters match nothing.

## Out of scope (v1)

- Server-side search / pagination
- Creating offers or comments from the drawer (use full public page)
- Keeping marketing landing content elsewhere
- Geocoding / clustering beyond simple multi-markers

## Tests

- Backend: discovery returns only ACTIVE points; needs only OPEN + PARTIALLY_COVERED; DTO-only response.
- Playwright: `/` shows map + list; free-text filters; pin click filters list; need opens drawer with link to `/r/...`. Replace obsolete landing e2e assertions.
