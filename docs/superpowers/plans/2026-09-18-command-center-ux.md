# Command-Center UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the NeedRelay Vite/Mantine frontend into an urgent Command-Center UX (Ink/Slate + signal red/amber) across Landing, Public Relief, Dashboard, Create, and Manage — without backend API changes.

**Architecture:** Keep Mantine + React Router. Introduce CSS design tokens and a remapped Mantine theme (Sora, ink primary). Extract small presentational/layout components and shared urgency sort helpers. Pages compose those components; Orval API clients stay unchanged. Offer KPI on dashboard is omitted in v1 (no N+1).

**Tech Stack:** React 19, Vite 6, Mantine 9, react-i18next, Leaflet/react-leaflet, Yarn 4, TypeScript 5.8, Vitest + Testing Library (add), Playwright (add)

**Spec:** `docs/superpowers/specs/2026-09-18-command-center-ux-design.md`

## Global Constraints

- English only in code/comments/commits; UI strings via i18n (`en.json` + `de.json`).
- No backend/OpenAPI changes for v1.
- No teal as primary; ink/slate primary; red=critical, amber=open/high, green=covered/success only.
- Font: **Sora** (Google Fonts) for brand and UI.
- Landing first viewport: brand dominant, one headline, one support line, one CTA group, live urgency panel — no hero cards.
- Public mobile: map on top, needs below.
- Dashboard KPIs: Critical + Open (+ Covered optional); **no global Offer KPI**.
- JSDoc on every new function.
- Zero unused imports/vars.
- Package manager: **Yarn 4** only (`yarn`, not npm).
- If `git status` fails (no repo), **skip commit steps** and continue.
- Verify each task: `yarn build` (and new test commands when added). Frontend page tasks need Playwright coverage by Task 9 at latest; helper/component tasks use Vitest in-task.

---

## File structure (locked)

| Path | Responsibility |
|------|----------------|
| `needrelay-frontend/index.html` | Load Sora font |
| `needrelay-frontend/src/styles/tokens.css` | CSS variables `--nr-*` |
| `needrelay-frontend/src/theme.ts` | Mantine theme (ink primary, Sora, radius) |
| `needrelay-frontend/src/lib/urgency.ts` | Sort/aggregate helpers |
| `needrelay-frontend/src/lib/urgency.test.ts` | Unit tests for helpers |
| `needrelay-frontend/src/components/AppLayout.tsx` | Public vs Ops chrome |
| `needrelay-frontend/src/components/UrgencyKpiStrip.tsx` | Dashboard KPI strip |
| `needrelay-frontend/src/components/RequestUrgencyRow.tsx` | Dashboard request row |
| `needrelay-frontend/src/components/PublicReliefSplit.tsx` | Map + needs responsive grid |
| `needrelay-frontend/src/components/NeedOfferRow.tsx` | Public need row + CTA |
| `needrelay-frontend/src/components/CreateRequestSplit.tsx` | Map \| form layout shell |
| `needrelay-frontend/src/components/ManageDualPane.tsx` | Needs/map \| offers layout |
| `needrelay-frontend/src/components/OfferInbox.tsx` | Offer list + actions UI |
| `needrelay-frontend/src/components/AuthPanel.tsx` | Shared auth page frame |
| `needrelay-frontend/src/pages/*.tsx` | Wire flows (Landing, Dashboard, Public, New, Manage, auth pages) |
| `needrelay-frontend/src/locales/en.json` / `de.json` | All new copy |
| `needrelay-frontend/vitest.config.ts` | Unit test config |
| `needrelay-frontend/playwright.config.ts` | E2E config |
| `needrelay-frontend/e2e/*.spec.ts` | Flow E2E tests |

---

### Task 1: Design tokens, Sora, Mantine theme, AppLayout ops chrome

**Files:**
- Create: `needrelay-frontend/src/styles/tokens.css`
- Modify: `needrelay-frontend/index.html`
- Modify: `needrelay-frontend/src/theme.ts`
- Modify: `needrelay-frontend/src/main.tsx` (import tokens)
- Modify: `needrelay-frontend/src/components/AppLayout.tsx`
- Modify: `needrelay-frontend/src/locales/en.json`, `needrelay-frontend/src/locales/de.json` (nav labels if renamed)
- Test: visual/`yarn build` this task; Playwright in Task 9

**Interfaces:**
- Consumes: existing `getAccessToken`, `authApi.me`, i18n
- Produces: CSS vars `--nr-ink`, `--nr-surface`, `--nr-signal`, `--nr-warn`, `--nr-ok`, `--nr-muted`; Mantine primary `ink`; AppLayout ops nav labels `nav.ops` → Dashboard

- [ ] **Step 1: Add Sora to `index.html`**

In `<head>`, before title:

```html
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Sora:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
```

- [ ] **Step 2: Create `tokens.css`**

```css
:root {
  --nr-ink: #0b1220;
  --nr-ink-2: #111827;
  --nr-surface: #f8fafc;
  --nr-signal: #e11d48;
  --nr-warn: #f59e0b;
  --nr-ok: #16a34a;
  --nr-muted: #64748b;
  --nr-panel: #1e293b;
}

body {
  margin: 0;
  background: var(--nr-surface);
}
```

- [ ] **Step 3: Replace `theme.ts`**

```ts
import { createTheme } from '@mantine/core';

/**
 * Command-Center Mantine theme (ink primary, Sora, signal-friendly neutrals).
 */
export const theme = createTheme({
  primaryColor: 'ink',
  fontFamily: 'Sora, Segoe UI, sans-serif',
  headings: {
    fontFamily: 'Sora, Segoe UI, sans-serif',
    fontWeight: '700',
  },
  defaultRadius: 'sm',
  colors: {
    ink: [
      '#f1f5f9',
      '#e2e8f0',
      '#cbd5e1',
      '#94a3b8',
      '#64748b',
      '#475569',
      '#334155',
      '#1e293b',
      '#0f172a',
      '#0b1220',
    ],
  },
  primaryShade: 9,
});
```

- [ ] **Step 4: Import tokens in `main.tsx`**

```ts
import './styles/tokens.css';
```

(keep existing App import)

- [ ] **Step 5: Update AppLayout nav for Ops mode**

When `loggedIn`:
- Link `/dashboard` label: `t('nav.ops')` (add EN: `"Ops"`, DE: `"Ops"`)
- Add prominent `t('nav.createRequest')` button → `/requests/new` (variant filled, color ink)
- Keep invites, account, admin, logout
- Header background: `var(--nr-ink)`, text/anchors light (`c="gray.0"` / white); language Select use light styles

When logged out: keep brand + language + login on ink or light bar — use ink header consistently for brand continuity.

- [ ] **Step 6: i18n keys**

`en.json` / `de.json`:
```json
"nav": {
  "ops": "Ops",
  ...
}
```
DE: `"ops": "Ops"`

- [ ] **Step 7: Build**

Run: `cd needrelay-frontend && yarn build`  
Expected: success (tsc + vite)

- [ ] **Step 8: Commit** (skip if no git)

```bash
git add needrelay-frontend/index.html needrelay-frontend/src/styles/tokens.css needrelay-frontend/src/theme.ts needrelay-frontend/src/main.tsx needrelay-frontend/src/components/AppLayout.tsx needrelay-frontend/src/locales/en.json needrelay-frontend/src/locales/de.json
git commit -m "feat(ui): add command-center theme tokens and ops shell"
```

---

### Task 2: Urgency helpers + Vitest

**Files:**
- Create: `needrelay-frontend/src/lib/urgency.ts`
- Create: `needrelay-frontend/src/lib/urgency.test.ts`
- Modify: `needrelay-frontend/package.json` (scripts + devDeps)
- Create: `needrelay-frontend/vitest.config.ts`

**Interfaces:**
- Consumes: `NeedResponse`, `NeedPriority`, `ReliefRequestSummaryResponse` from generated models
- Produces:
  - `priorityRank(priority: NeedPriority): number` — CRITICAL=0 … LOW=3
  - `sortNeedsByUrgency(needs: NeedResponse[]): NeedResponse[]` — priority asc rank, then `remaining` desc
  - `sortSummariesByUrgency(items: ReliefRequestSummaryResponse[]): ReliefRequestSummaryResponse[]` — `criticalNeeds` desc, then `openNeeds` desc
  - `sumSummaryCounts(items: ReliefRequestSummaryResponse[]): { critical: number; open: number; covered: number }`

- [ ] **Step 1: Add Vitest deps and script**

```bash
cd needrelay-frontend && yarn add -D vitest @testing-library/react @testing-library/jest-dom jsdom
```

`package.json` scripts:
```json
"test": "vitest run",
"test:watch": "vitest"
```

`vitest.config.ts`:
```ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
  },
});
```

- [ ] **Step 2: Write failing tests `urgency.test.ts`**

```ts
import { describe, expect, it } from 'vitest';
import { NeedPriority } from '../api/generated/models';
import {
  priorityRank,
  sortNeedsByUrgency,
  sortSummariesByUrgency,
  sumSummaryCounts,
} from './urgency';

describe('priorityRank', () => {
  it('orders critical before low', () => {
    expect(priorityRank(NeedPriority.CRITICAL)).toBeLessThan(priorityRank(NeedPriority.LOW));
  });
});

describe('sortNeedsByUrgency', () => {
  it('puts critical with higher remaining first among same priority', () => {
    const sorted = sortNeedsByUrgency([
      {
        id: '1',
        title: 'a',
        category: 'SUPPLIES',
        quantityRequired: 10,
        quantityOffered: 0,
        remaining: 5,
        unit: 'u',
        priority: NeedPriority.CRITICAL,
        status: 'OPEN',
      },
      {
        id: '2',
        title: 'b',
        category: 'SUPPLIES',
        quantityRequired: 10,
        quantityOffered: 0,
        remaining: 9,
        unit: 'u',
        priority: NeedPriority.CRITICAL,
        status: 'OPEN',
      },
      {
        id: '3',
        title: 'c',
        category: 'SUPPLIES',
        quantityRequired: 1,
        quantityOffered: 0,
        remaining: 1,
        unit: 'u',
        priority: NeedPriority.NORMAL,
        status: 'OPEN',
      },
    ] as never);
    expect(sorted.map((n) => n.id)).toEqual(['2', '1', '3']);
  });
});

describe('sortSummariesByUrgency', () => {
  it('sorts by critical then open', () => {
    const sorted = sortSummariesByUrgency([
      { id: 'a', title: 'a', locationLabel: '', publicSlug: 'a', openNeeds: 9, criticalNeeds: 0, coveredNeeds: 0 },
      { id: 'b', title: 'b', locationLabel: '', publicSlug: 'b', openNeeds: 1, criticalNeeds: 2, coveredNeeds: 0 },
    ]);
    expect(sorted.map((s) => s.id)).toEqual(['b', 'a']);
  });
});

describe('sumSummaryCounts', () => {
  it('sums critical open covered', () => {
    expect(
      sumSummaryCounts([
        { id: '1', title: '', locationLabel: '', publicSlug: '', openNeeds: 2, criticalNeeds: 1, coveredNeeds: 3 },
        { id: '2', title: '', locationLabel: '', publicSlug: '', openNeeds: 4, criticalNeeds: 2, coveredNeeds: 1 },
      ]),
    ).toEqual({ critical: 3, open: 6, covered: 4 });
  });
});
```

(Adjust `category`/`status` enums to match generated `NeedCategory` / `NeedStatus` imports if the `as never` cast is insufficient — prefer real enum values.)

- [ ] **Step 3: Run tests — expect FAIL**

Run: `cd needrelay-frontend && yarn test`  
Expected: FAIL (module `./urgency` missing)

- [ ] **Step 4: Implement `urgency.ts`**

```ts
import type { NeedResponse, ReliefRequestSummaryResponse } from '../api/generated/models';
import { NeedPriority } from '../api/generated/models';

const RANK: Record<NeedPriority, number> = {
  [NeedPriority.CRITICAL]: 0,
  [NeedPriority.HIGH]: 1,
  [NeedPriority.NORMAL]: 2,
  [NeedPriority.LOW]: 3,
};

/**
 * Maps need priority to a sortable rank (lower = more urgent).
 *
 * @param priority need priority
 * @returns numeric rank
 */
export function priorityRank(priority: NeedPriority): number {
  return RANK[priority];
}

/**
 * Sorts needs by urgency: priority rank ascending, then remaining descending.
 *
 * @param needs need list
 * @returns new sorted array
 */
export function sortNeedsByUrgency(needs: NeedResponse[]): NeedResponse[] {
  return [...needs].sort((a, b) => {
    const byPriority = priorityRank(a.priority) - priorityRank(b.priority);
    if (byPriority !== 0) return byPriority;
    return b.remaining - a.remaining;
  });
}

/**
 * Sorts dashboard summaries by criticalNeeds then openNeeds (descending).
 *
 * @param items summary list
 * @returns new sorted array
 */
export function sortSummariesByUrgency(
  items: ReliefRequestSummaryResponse[],
): ReliefRequestSummaryResponse[] {
  return [...items].sort((a, b) => {
    if (b.criticalNeeds !== a.criticalNeeds) return b.criticalNeeds - a.criticalNeeds;
    return b.openNeeds - a.openNeeds;
  });
}

/**
 * Aggregates KPI totals from relief request summaries.
 *
 * @param items summary list
 * @returns summed critical/open/covered counts
 */
export function sumSummaryCounts(items: ReliefRequestSummaryResponse[]): {
  critical: number;
  open: number;
  covered: number;
} {
  return items.reduce(
    (acc, item) => ({
      critical: acc.critical + item.criticalNeeds,
      open: acc.open + item.openNeeds,
      covered: acc.covered + item.coveredNeeds,
    }),
    { critical: 0, open: 0, covered: 0 },
  );
}
```

- [ ] **Step 5: Run tests — expect PASS**

Run: `yarn test`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add needrelay-frontend/package.json needrelay-frontend/yarn.lock needrelay-frontend/vitest.config.ts needrelay-frontend/src/lib/urgency.ts needrelay-frontend/src/lib/urgency.test.ts
git commit -m "feat(ui): add urgency sort helpers and vitest"
```

---

### Task 3: Landing page (Split B)

**Files:**
- Modify: `needrelay-frontend/src/pages/LandingPage.tsx`
- Modify: `needrelay-frontend/src/locales/en.json`, `de.json`
- Test: Playwright in Task 9 (`data-testid="landing-brand"`, `landing-cta`, `landing-live-panel`)

**Interfaces:**
- Consumes: theme tokens, i18n
- Produces: Landing markup with testids above; CTA → `/login`

- [ ] **Step 1: Add i18n keys**

EN:
```json
"landing": {
  "headline": "See what's missing. Close the gap.",
  "support": "Built for organizers under pressure.",
  "ctaOrganizer": "Organizer login",
  "liveTitle": "Live snapshot",
  "liveCritical": "Critical needs",
  "liveOpen": "Open",
  "liveCovered": "Covered",
  "liveCriticalSample": "3",
  "liveOpenSample": "12",
  "liveCoveredSample": "8"
}
```
(DE translations required — equivalent meaning.)  
Note: sample numbers are **illustrative marketing only** on logged-out landing (not live API). Keep copy honest in DE/EN that this is an example panel, OR label `landing.liveExample`: `"Example"` / `"Beispiel"`.

- [ ] **Step 2: Implement LandingPage**

Structure:
- Full-width section with subtle gradient `linear-gradient(135deg, #0b1220 0%, #1e293b 55%, #0f172a 100%)`, light text
- CSS grid `1.1fr 0.9fr` desktop; single column mobile (brand block first, panel second)
- Left: `data-testid="landing-brand"` Title order=1 **NeedRelay** (largest), headline (`order={2}` smaller than brand), support Text, Button CTA `data-testid="landing-cta"` → `/login` color signal (`style={{ background: 'var(--nr-signal)' }}` or Mantine `color="red"`)
- Right: `data-testid="landing-live-panel"` dark panel with three rows bordered left critical/warn/ok — **no Card component in hero**
- No extra marketing sections in first viewport

- [ ] **Step 3: Build**

Run: `yarn build`  
Expected: success

- [ ] **Step 4: Commit**

```bash
git commit -am "feat(ui): redesign landing as brand + urgency panel"
```

---

### Task 4: Dashboard Ops (KPI strip + urgency rows)

**Files:**
- Create: `needrelay-frontend/src/components/UrgencyKpiStrip.tsx`
- Create: `needrelay-frontend/src/components/RequestUrgencyRow.tsx`
- Modify: `needrelay-frontend/src/pages/DashboardPage.tsx`
- Modify: locales
- Create: `needrelay-frontend/src/components/UrgencyKpiStrip.test.tsx` (render test)

**Interfaces:**
- Consumes: `sumSummaryCounts`, `sortSummariesByUrgency`, `ReliefRequestSummaryResponse`
- Produces:
  - `UrgencyKpiStrip({ critical, open, covered }: { critical: number; open: number; covered: number })`
  - `RequestUrgencyRow({ item: ReliefRequestSummaryResponse })`

- [ ] **Step 1: Write failing render test for KPI strip**

```tsx
import { MantineProvider } from '@mantine/core';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { UrgencyKpiStrip } from './UrgencyKpiStrip';
import { theme } from '../theme';

describe('UrgencyKpiStrip', () => {
  it('shows critical open covered values', () => {
    render(
      <MantineProvider theme={theme}>
        <UrgencyKpiStrip critical={5} open={14} covered={8} />
      </MantineProvider>,
    );
    expect(screen.getByTestId('kpi-critical')).toHaveTextContent('5');
    expect(screen.getByTestId('kpi-open')).toHaveTextContent('14');
    expect(screen.getByTestId('kpi-covered')).toHaveTextContent('8');
  });
});
```

- [ ] **Step 2: Run `yarn test` — expect FAIL** (component missing)

- [ ] **Step 3: Implement `UrgencyKpiStrip`**

Three metric blocks with top border colors signal/warn/ok; labels via i18n `dashboard.kpiCritical` etc.; wrap in Group/SimpleGrid; optional CSS `@keyframes nrKpiIn` opacity/translateY once on mount (motion #1).

- [ ] **Step 4: Implement `RequestUrgencyRow`**

Row (not heavy Card chrome unless needed for click targets): title, location, badges for critical/open/covered counts, links Manage + Public. Left border `var(--nr-signal)` if `criticalNeeds > 0` else warn if `openNeeds > 0` else muted. `data-testid={`request-row-${item.id}`}`.

- [ ] **Step 5: Rewrite `DashboardPage`**

- Load list as today
- `const sorted = sortSummariesByUrgency(items)`
- `const totals = sumSummaryCounts(items)`
- Header: title + New CTA
- `<UrgencyKpiStrip {...totals} />` with `data-testid="dashboard-kpis"`
- Empty: dimmed text + CTA
- Else map `RequestUrgencyRow`
- Loading: Text or Skeleton

- [ ] **Step 6: i18n** `dashboard.kpiCritical`, `kpiOpen`, `kpiCovered`

- [ ] **Step 7: `yarn test` + `yarn build` — PASS

- [ ] **Step 8: Commit**

```bash
git commit -am "feat(ui): ops dashboard with KPI strip and urgency sort"
```

---

### Task 5: Public Relief (Map + needs; mobile stack)

**Files:**
- Create: `needrelay-frontend/src/components/NeedOfferRow.tsx`
- Create: `needrelay-frontend/src/components/PublicReliefSplit.tsx`
- Modify: `needrelay-frontend/src/pages/PublicReliefPage.tsx`
- Modify: locales if needed

**Interfaces:**
- Consumes: `sortNeedsByUrgency`, `LocationMap`, `NeedResponse`
- Produces:
  - `NeedOfferRow({ need, onOffer: (need) => void })`
  - `PublicReliefSplit({ map, list }: { map: ReactNode; list: ReactNode })` — desktop 2-col, mobile column map-first

- [ ] **Step 1: Implement `PublicReliefSplit`**

```tsx
import { Box, SimpleGrid } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * Responsive public layout: map + needs; on mobile map stacks above needs.
 *
 * @param props.map map node
 * @param props.list needs list node
 * @returns layout wrapper
 */
export function PublicReliefSplit({ map, list }: { map: ReactNode; list: ReactNode }) {
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" data-testid="public-relief-split">
      <Box data-testid="public-relief-map">{map}</Box>
      <Box data-testid="public-relief-needs">{list}</Box>
    </SimpleGrid>
  );
}
```

(On `base: 1`, first child = map — satisfies mobile map-on-top.)

- [ ] **Step 2: Implement `NeedOfferRow`**

- Emphasize `remaining` (large, color signal if CRITICAL else warn/ink)
- Status badge + priority
- Offer button `data-testid={`need-offer-${need.id}`}`
- Left border by priority
- Optional brief CSS animation for critical rows (motion #2)

- [ ] **Step 3: Wire `PublicReliefPage`**

- Keep load + modal offer submit logic
- Header: title, location, description above split
- `sorted = sortNeedsByUrgency(data.needs)`
- Pass `<LocationMap mode="readonly" .../>` as map; mapped `NeedOfferRow` as list
- Remaining color: use `var(--nr-signal)` not teal

- [ ] **Step 4: `yarn build`**

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(ui): public relief map-needs split with urgency sort"
```

---

### Task 6: Create Request split (+ optional first need)

**Files:**
- Create: `needrelay-frontend/src/components/CreateRequestSplit.tsx`
- Modify: `needrelay-frontend/src/pages/NewRequestPage.tsx`
- Modify: locales

**Interfaces:**
- Consumes: `reliefRequestsApi.createReliefRequest`, `needsApi.createNeed` (verify export from `src/api`)
- Produces: `CreateRequestSplit({ map, form }: { map: ReactNode; form: ReactNode })`

- [ ] **Step 1: Confirm `needsApi.createNeed` exists in `src/api`**

If the wrapper name differs, use the Orval function that POSTs a need under a request id (same as ManagePage add-need).

- [ ] **Step 2: Implement `CreateRequestSplit`**

Same pattern as Public: `SimpleGrid cols={{ base: 1, sm: 2 }}` with map first for mobile.

- [ ] **Step 3: Extend `NewRequestPage` form values**

```ts
{
  title: '',
  description: '',
  locationLabel: '',
  latitude: 36.2023,
  longitude: 36.1613,
  firstNeedEnabled: false,
  firstNeedTitle: '',
  firstNeedQuantity: 1,
  firstNeedUnit: 'units',
  firstNeedPriority: NeedPriority.NORMAL,
}
```

UI:
- Split: map picker | fields (title, description, locationLabel; hide raw lat/lng NumberInputs behind optional collapse or keep compact under map)
- Checkbox/Switch `request.addFirstNeed` → show first-need fields
- Submit:
  1. `createReliefRequest` core fields
  2. If first need enabled and title non-empty → `createNeed(created.id, { ... })`
  3. `navigate(/requests/${created.id})`

- [ ] **Step 4: i18n** for first-need block labels

- [ ] **Step 5: `yarn build`**

- [ ] **Step 6: Commit**

```bash
git commit -am "feat(ui): create request split layout with optional first need"
```

---

### Task 7: Manage dual pane (Needs+Map | Offer inbox)

**Files:**
- Create: `needrelay-frontend/src/components/OfferInbox.tsx`
- Create: `needrelay-frontend/src/components/ManageDualPane.tsx`
- Modify: `needrelay-frontend/src/pages/ManageRequestPage.tsx` (large — restructure, keep existing API handlers)
- Modify: locales as needed

**Interfaces:**
- Consumes: existing reload/handlers in ManageRequestPage; `sortNeedsByUrgency`
- Produces:
  - `ManageDualPane({ left, right }: { left: ReactNode; right: ReactNode })` — `SimpleGrid cols={{ base: 1, md: 2 }}`
  - `OfferInbox({ offers, onEdit, onDelete }: { offers: OfferResponse[]; onEdit: (o: OfferResponse) => void; onDelete: (id: string) => void })`

- [ ] **Step 1: Implement `ManageDualPane` + `OfferInbox`**

`OfferInbox`: list rows with provider, quantity, contact, note excerpt; Edit/Delete buttons; empty state i18n; `data-testid="offer-inbox"`; light enter animation on list (motion #3).

- [ ] **Step 2: Restructure ManageRequestPage layout**

Order:
1. Title + public link code
2. `ManageDualPane`:
   - **Left:** sorted needs (reuse table or urgency rows with edit actions) + LocationMap readonly/picker as currently for location
   - **Right:** `OfferInbox` + existing edit offer modal wiring
3. **Below** (Accordion default collapsed or second section): location/metadata form + internal comments — must not sit above triage panes

Preserve all existing save/create/edit/delete handlers; only rearrange JSX and sort needs via `sortNeedsByUrgency`.

Mobile order via grid: left stack (needs then map) then right (offers) — if map is inside left column below needs, mobile becomes Needs → Map → Offers → Notes ✓

- [ ] **Step 3: `yarn build`**

- [ ] **Step 4: Commit**

```bash
git commit -am "feat(ui): manage request dual-pane triage and offer inbox"
```

---

### Task 8: Auth / Account / Invites / Admin polish

**Files:**
- Create: `needrelay-frontend/src/components/AuthPanel.tsx`
- Modify: `LoginPage.tsx`, `RegisterPage.tsx`, `ForgotPasswordPage.tsx`, `ResetPasswordPage.tsx`, `AccountPage.tsx`, `InvitesPage.tsx`, `AdminOrganizersPage.tsx`

**Interfaces:**
- Consumes: theme tokens
- Produces: `AuthPanel({ title, children }: { title: string; children: ReactNode })` — narrow container, ink top border or dark header strip

- [ ] **Step 1: Implement `AuthPanel`**

```tsx
import { Container, Paper, Stack, Title } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * Shared frame for auth and simple settings forms.
 *
 * @param props.title page title
 * @param props.children form content
 * @returns framed panel
 */
export function AuthPanel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Container size="xs" py="xl">
      <Paper p="lg" radius="sm" style={{ borderTop: '3px solid var(--nr-signal)' }}>
        <Stack>
          <Title order={2}>{title}</Title>
          {children}
        </Stack>
      </Paper>
    </Container>
  );
}
```

- [ ] **Step 2: Wrap auth pages with `AuthPanel`**; primary buttons use ink/red consistently; remove teal leftovers.

- [ ] **Step 3: Light polish Account/Invites/Admin** — Container + Title spacing, signal accents on destructive actions (ban), no layout redesign beyond theme consistency.

- [ ] **Step 4: `yarn build`**

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(ui): polish auth and secondary pages to command-center theme"
```

---

### Task 9: Playwright setup + E2E smoke

**Files:**
- Modify: `needrelay-frontend/package.json`
- Create: `needrelay-frontend/playwright.config.ts`
- Create: `needrelay-frontend/e2e/landing.spec.ts`
- Create: `needrelay-frontend/e2e/public-relief-layout.spec.ts`
- Create: `needrelay-frontend/e2e/dashboard-shell.spec.ts` (auth-dependent — skip or use storage if seed unavailable; document)

**Interfaces:**
- Consumes: `data-testid`s from prior tasks
- Produces: green Playwright suite for static layout assertions; auth flows only if seed credentials documented in README/env

- [ ] **Step 1: Install Playwright**

```bash
cd needrelay-frontend && yarn add -D @playwright/test && yarn playwright install chromium
```

Scripts:
```json
"test:e2e": "playwright test",
"test:e2e:headed": "playwright test --headed"
```

`playwright.config.ts`:
```ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'yarn dev --host 127.0.0.1 --port 5173',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: true,
  },
});
```

- [ ] **Step 2: `e2e/landing.spec.ts`**

```ts
import { expect, test } from '@playwright/test';

test('landing shows brand panel and organizer CTA', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('landing-brand')).toBeVisible();
  await expect(page.getByTestId('landing-live-panel')).toBeVisible();
  await page.getByTestId('landing-cta').click();
  await expect(page).toHaveURL(/\/login/);
});
```

- [ ] **Step 3: `e2e/public-relief-layout.spec.ts`**

Use a known public slug from seed **or** skip with explicit message if none. Prefer: backend seed creates one; if not available, test only DOM order with route mocked — **do not** fake pass. Minimum without API:

If public page 404s without data, add test that mounts layout via Story-less approach: navigate `/` only for landing; for public, document required seed slug in test as `process.env.E2E_PUBLIC_SLUG` and `test.skip(!process.env.E2E_PUBLIC_SLUG, 'needs seed')`.

With slug:
```ts
test('public relief stacks map above needs on mobile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`/r/${process.env.E2E_PUBLIC_SLUG}`);
  const map = page.getByTestId('public-relief-map');
  const needs = page.getByTestId('public-relief-needs');
  await expect(map).toBeVisible();
  await expect(needs).toBeVisible();
  const mapBox = await map.boundingBox();
  const needsBox = await needs.boundingBox();
  expect(mapBox && needsBox && mapBox.y < needsBox.y).toBeTruthy();
});
```

- [ ] **Step 4: Dashboard shell** — only if `E2E_EMAIL` / `E2E_PASSWORD` set: login → expect `dashboard-kpis`. Else skip with reason.

- [ ] **Step 5: Run**

```bash
yarn test
yarn build
yarn test:e2e
```

Expected: unit tests pass; landing e2e pass; others pass or intentional skip with env note in README.

- [ ] **Step 6: Update root `README.md` frontend section** with `yarn test` / `yarn test:e2e` and optional E2E env vars.

- [ ] **Step 7: Commit**

```bash
git commit -am "test(ui): add vitest/playwright coverage for command-center UX"
```

- [ ] **Step 8: Run verifier subagent** on the full frontend UX change set; fix any findings.

---

## Self-review (plan vs spec)

| Spec requirement | Task |
|------------------|------|
| Ink/Slate + signal colors, drop teal primary | 1 |
| Sora | 1 |
| AppLayout public vs ops | 1 |
| Landing B split + live panel | 3 |
| Dashboard KPI Critical/Open/Covered, no Offer KPI | 4 |
| Urgency sort summaries/needs | 2, 4, 5, 7 |
| Public map+needs; mobile map top | 5 |
| Create split + optional first need | 6 |
| Manage dual pane + offer inbox; notes secondary | 7 |
| Auth polish | 8 |
| i18n EN+DE | 1–8 |
| Motion 2–3 | 4, 5, 7 |
| Playwright + build verification | 9 |
| No backend changes | Global |

No remaining TBD placeholders. Types aligned on `ReliefRequestSummaryResponse.openNeeds` / `criticalNeeds` / `coveredNeeds` (frontend generated names).

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-18-command-center-ux.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks  
2. **Inline Execution** — execute tasks in this session with checkpoints  

Which approach?
