# Mobile-friendly UI (scope C)

**Date:** 2026-09-18  
**Status:** Approved for implementation  
**Scope:** Frontend only (`needrelay-frontend`)

## Goal

Make the organizer and public UI usable on narrow viewports: no header overflow, readable lists instead of cramped tables, adequate touch targets, and Playwright coverage on a mobile device profile.

## Locked decisions

| Topic | Choice |
| --- | --- |
| Scope | **C** — shell + main pages + tables + touch targets + Playwright mobile |
| Tables under `sm` | **Stacked cards/rows** (no table layout); `Table` from `sm` up |
| Breakpoint | Mantine **`sm`** (`visibleFrom` / `hiddenFrom`) |
| Nav under `sm` | Burger + Drawer with the same links as the desktop header |
| Language Select | Stays in the header (searchable, constrained width) |
| Touch targets | Prefer **≥ 44px** interactive height (theme defaults + bump `xs`/`compact` on mobile where needed) |
| Playwright | Add **Pixel 5** project; cover burger nav, discovery usable, one table→cards page, logout still passes |

## Shell (`AppLayout`)

- **`< sm`:** Brand + language select + **Burger**; nav actions live in a **Drawer** (full-width buttons/links: Ops, New relief request, Invites, Account, Admin if applicable, Logout / Login). Drawer closes on navigate.
- **`≥ sm`:** Existing inline header buttons (unchanged behavior).
- Logout confirmation modal unchanged; logout entry available from drawer when logged in.
- Header height may increase slightly if needed for 44px controls; keep single-row header.

## Tables → cards

Pages with `Table` today:

1. `ManageRequestPage` (needs list)
2. `InvitesPage`
3. `AdminOrganizersPage`

For each:

- **`hiddenFrom="sm"`:** Stack of cards/rows showing the same fields and actions (labels + values).
- **`visibleFrom="sm"`:** Existing `Table`.

Prefer small local markup per page unless two pages share an identical pattern (then extract once).

## Touch targets / theme

- Theme defaults: `Button` / primary controls toward `md` size where it does not break dense desktop toolbars.
- On mobile card rows and drawer items: avoid `size="xs"` / `compact-xs` for primary actions; use at least `sm`.
- Offer inbox / need row actions: ensure tap targets are usable on Pixel-width viewports (stack actions vertically under `sm` if a horizontal `Group` overflows).

## Pages already mostly stacked

Discovery, Public relief, Dashboard, forms: verify no horizontal overflow; fix only clear breakages (map height, wrapping groups). No redesign.

## Playwright

- `playwright.config.ts`: add project `mobile` with `devices['Pixel 5']` (keep existing Desktop Chrome).
- New/updated e2e:
  - Burger opens drawer; a nav link works (e.g. Log in visible when logged out).
  - Discovery page loads on mobile viewport.
  - One admin/manage or invites surface shows card layout markers (`data-testid` on mobile list) when on mobile project.
  - Existing logout spec runs on mobile project (or shared).

## Non-goals

- Dark mode, new visual brand, backend changes, desktop layout overhauls, Kurdish/new locales.

## Expected files

- `AppLayout.tsx` — burger + drawer
- `theme.ts` — touch-friendly defaults as needed
- `ManageRequestPage.tsx`, `InvitesPage.tsx`, `AdminOrganizersPage.tsx` — dual table/card
- Possibly small shared `MobileCardList` only if duplication warrants it
- `playwright.config.ts` + `e2e/mobile-shell.spec.ts` (name flexible)
- i18n: only if new visible strings (e.g. “Menu” aria) — prefer aria-labels already in English or reuse `nav.*` keys
