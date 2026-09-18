# NeedRelay Command-Center UX Redesign

**Date:** 2026-09-18  
**Status:** Approved for planning  
**Scope:** Full frontend UX redesign (public + organizer), Mantine retained, no backend API changes required for v1

## Goals

- Make the product feel **urgent and action-oriented** (scan critical gaps, act fast).
- Replace calm teal marketing UI with **Ink/Slate + signal red/amber**.
- Redesign core flows: Landing, Public Relief, Dashboard, Create Request, Manage Request.
- Auth / Account / Invites / Admin: same visual system, lighter layout polish only.

## Non-goals

- Backend/OpenAPI contract changes (unless later discovered necessary).
- Dark-mode toggle.
- Map-as-primary spatial UX everywhere.
- Wizard-only create flow.
- New product features unrelated to presentation/flow of existing capabilities.

## Decisions (locked)

| Surface | Choice |
|---------|--------|
| Approach | Command-Center |
| Landing | Split: Brand + live urgency panel |
| Public Relief | Map + needs list; **mobile: map on top, needs below** |
| Organizer shell | Ops dashboard with KPI strip |
| Manage Request | Dual pane: Needs + Map \| Offer inbox |
| Create Request | Split: Map \| Form (+ optional first need) |
| Color | Deep ink/slate primary; red = critical; amber = open/high; green = covered/success only; teal dropped as primary |

## Visual system

### Color tokens (CSS variables + Mantine theme)

- `--nr-ink` — near-black blue/slate for brand bars and dark panels
- `--nr-surface` — light ops background
- `--nr-signal` — critical red
- `--nr-warn` — amber for elevated/open
- `--nr-ok` — success/covered green
- `--nr-muted` — secondary text

Mantine `primaryColor` remapped to ink/slate scale; status badges use dedicated colors (not primary).

### Typography

- Display/brand: **Sora** (Google Fonts).
- UI body: **Sora** at regular/medium weights for density and readability (single family; weight hierarchy only).
- Hierarchy: brand on Landing must dominate the first viewport; headlines must not overpower the brand name.

### Atmosphere & motion

- Landing: subtle depth (gradient / soft texture), not flat white.
- Organizer: denser, high-contrast, minimal decoration.
- Ship 2–3 intentional motions: KPI appear, critical need emphasis, offer inbox row enter — no glow stacks, no purple themes.

### Layout rules (from product design constraints)

- Landing first viewport: brand, one headline, one short support line, one CTA group, live panel as the visual anchor — no stat-strip clutter beyond the agreed panel.
- No cards in the Landing hero; cards only where they support interaction (need offer, request row actions).
- One job per section.

## Information architecture

### Public vs authenticated shell

- Shared `AppLayout` with mode:
  - **Public:** brand + language + login
  - **Ops (logged in):** brand + Ops (dashboard) + New + Invites + Account (+ Admin if role) + logout
- Language switcher remains global.

### Dashboard (Ops)

1. KPI strip: Critical needs count, Open needs count, Pending/new offers count (aggregate client-side from existing `ReliefRequestSummaryResponse` fields where possible; offers may be approximate or per-request until a global inbox API exists — v1: show per-dashboard sums of critical/open/covered from summaries; offer KPI = sum only if list endpoint available without N+1, otherwise omit offer KPI or show “—” with copy that offers are managed per request).
2. Request list sorted by urgency (`criticalNeeds` desc, then `openNeeds` desc).
3. Primary CTA: create request.

### Public Relief (`/r/:slug`)

- Desktop: two columns — map dominant left/top-left, needs list right.
- Mobile (`< sm`): **map on top**, needs stacked below.
- Needs sorted: critical priority first, then remaining quantity descending.
- Offer: keep modal form; CTA per need row/card.
- Remaining quantity is the dominant number.

### Create Request (`/requests/new`)

- Desktop split: map picker | form (title, description, location label, lat/lng synced).
- Optional block: “Add first need” (title, qty, unit, priority) — if filled, create request then create need before navigate.
- Submit → navigate to Manage for that request.
- Mobile: map on top, form below (same stack pattern as Public).

### Manage Request (`/requests/:id`)

- Desktop dual pane:
  - **Left:** needs list (critical first, remaining qty emphasized) + map
  - **Right:** offer inbox (list + accept/edit actions already supported by API)
- Secondary: request metadata edit + internal notes in a collapsible/lower section (not competing with triage).
- Mobile: stack Needs → Map → Offers → Notes.

## Component plan

| Component | Responsibility |
|-----------|----------------|
| `theme.ts` + global CSS vars | Tokens, Mantine theme |
| `UrgencyKpiStrip` | Dashboard KPIs |
| `RequestUrgencyRow` | Dashboard request row with critical/open/covered |
| `PublicReliefSplit` | Map + needs responsive layout |
| `NeedOfferRow` | Public need row with remaining + offer CTA |
| `CreateRequestSplit` | Map + form + optional first need |
| `ManageDualPane` | Needs/map \| offers |
| `OfferInbox` | Offer list actions |
| Auth pages polish | Shared `AuthPanel` layout within theme |

Extract shared sort helpers (e.g. `sortNeedsByUrgency`) used in Public + Manage.

## Data & error handling

- Reuse existing Orval clients; no DTO changes for v1.
- Loading: skeleton or compact loaders in panes (avoid blank full-page flash).
- Errors: Mantine notifications with i18n keys; keep failing panes recoverable via retry where practical.
- Empty states: actionable copy (e.g. dashboard empty → CTA create; public no open needs → covered message).

## i18n

- All new UI strings in `en.json` and `de.json`.
- No hardcoded user-facing English in components.

## Testing / verification

- `yarn build` + lint for frontend.
- Playwright: Landing primary CTA; Public mobile stack (map then needs); Dashboard KPI visible when data present; Create split submit; Manage dual-pane / stacked mobile smoke.
- Verifier subagent after non-trivial multi-page work.

## Implementation order (for planning)

1. Theme tokens + fonts + AppLayout ops chrome  
2. Landing B  
3. Dashboard A (KPI + urgency sort)  
4. Public Relief C + mobile stack  
5. Create Request C  
6. Manage Request B  
7. Auth/Account/Invites/Admin theme polish  
8. Playwright coverage + verification  

## Open implementation detail (resolved default)

- **Offer KPI on dashboard:** If computing offers requires one request per relief request, **omit global offer KPI in v1** and keep Critical + Open (+ Covered optional). Offers remain first-class on Manage. Do not introduce N+1 fetches for a vanity metric.
