# Mobile-friendly UI Implementation Plan

> **For agentic workers:** Execute task-by-task. Steps use checkbox syntax.

**Goal:** Burger/drawer nav under `sm`, stacked card lists instead of tables on mobile, ≥44px touch targets, Playwright Pixel 5 coverage.

**Architecture:** Mantine `visibleFrom`/`hiddenFrom`/`sm` breakpoint. AppLayout owns shell; each table page duplicates content as card stack for mobile. Theme bumps default control sizes.

**Tech Stack:** Mantine 9, React Router, Playwright `devices['Pixel 5']`

**Spec:** `docs/superpowers/specs/2026-09-18-mobile-friendly-design.md`

## Global Constraints

- Frontend only; English code/comments.
- Tables → cards under `sm` only; desktop tables unchanged.
- No commits unless asked.

---

### Task 1: Theme touch defaults + AppLayout burger/drawer

**Files:** `theme.ts`, `AppLayout.tsx`

- Button/ActionIcon default size `md`; Input default size `md`.
- Burger `hiddenFrom="sm"` / desktop nav `visibleFrom="sm"`; Drawer with full-width nav; `data-testid="nav-burger"`.

### Task 2: Table pages → mobile cards

**Files:** `InvitesPage.tsx`, `AdminOrganizersPage.tsx`, `ManageRequestPage.tsx`

- Table `visibleFrom="sm"`; Stack cards `hiddenFrom="sm"` with `data-testid="mobile-card-list"`.
- Mobile action buttons `size="sm"` min.

### Task 3: Playwright mobile project + specs

**Files:** `playwright.config.ts`, `e2e/mobile-shell.spec.ts`

- Project `mobile` = Pixel 5.
- Burger → drawer → Log in; discovery page; invites card list when mocked logged-in optional — prefer public discovery + burger login path; mock invites page with tokens for card list OR use admin with mocks.

### Task 4: Verify

`yarn build && yarn test && yarn test:e2e`
