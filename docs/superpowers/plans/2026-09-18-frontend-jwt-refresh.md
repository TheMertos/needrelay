# Frontend JWT Refresh + Logout Revoke Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (or subagent-driven-development) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Axios 401→refresh→retry (single-flight) and best-effort logout revoke on the NeedRelay frontend.

**Architecture:** Extend `src/api/client.ts` with a response interceptor that refreshes via a bare Axios POST (avoid circular Orval import). Register a session-expired redirect callback from `App.tsx`. Logout in `AppLayout` calls `authApi.logout` then clears tokens.

**Tech Stack:** Axios, Orval `authApi`, Vitest, Playwright, React Router

**Spec:** `docs/superpowers/specs/2026-09-18-frontend-jwt-refresh-design.md`

## Global Constraints

- Frontend only; no backend/OpenAPI changes.
- English-only code/comments.
- Refresh uses raw Axios to `/api/auth/refresh` (not `authApi`) to avoid circular imports and interceptor recursion.
- Skip refresh for auth URLs containing `/api/auth/login|register|refresh|logout|forgot-password|reset-password`.
- On refresh failure: `clearTokens()`; redirect `/login` only if `isProtectedPath(location.pathname)`.
- Logout: best-effort revoke, always clear + navigate `/`.
- No commits unless user asks.

---

### Task 1: Path helper + session-expired callback

**Files:**
- Create: `needrelay-frontend/src/api/session.ts`
- Create: `needrelay-frontend/src/api/session.test.ts`
- Modify: `needrelay-frontend/src/App.tsx` (register redirect on mount)

**Produces:**
- `isProtectedPath(pathname: string): boolean`
- `isAuthSkipUrl(url: string | undefined): boolean`
- `setSessionExpiredHandler(handler: (() => void) | null): void`
- `notifySessionExpired(): void`

- [ ] **Step 1:** Write `session.test.ts` for protected vs public paths and auth skip URLs.
- [ ] **Step 2:** Implement `session.ts`; in `App.tsx` inside `BrowserRouter`, a small `SessionBridge` component registers `() => navigate('/login', { replace: true })` and clears on unmount.
- [ ] **Step 3:** `yarn test src/api/session.test.ts` — pass.

---

### Task 2: Axios refresh interceptor (TDD)

**Files:**
- Modify: `needrelay-frontend/src/api/client.ts`
- Create: `needrelay-frontend/src/api/client.refresh.test.ts`

**Produces:** Response interceptor with single-flight refresh + one retry (`config._retry`).

- [ ] **Step 1:** Write failing tests (mock Axios adapter / interceptors) covering: refresh success + retry; failed refresh clears tokens + calls notify when protected; skip on login 401; no double refresh for parallel 401s.
- [ ] **Step 2:** Implement interceptor in `client.ts` using bare `Axios.post` to `${baseURL}/api/auth/refresh` with `{ refreshToken }`, then `setTokens`, retry original with new Authorization.
- [ ] **Step 3:** `yarn test src/api/client.refresh.test.ts` — pass.

---

### Task 3: Logout revoke

**Files:**
- Modify: `needrelay-frontend/src/components/AppLayout.tsx`
- Modify: `needrelay-frontend/e2e/logout.spec.ts`

- [ ] **Step 1:** `confirmLogout` async: if refresh token, `await authApi.logout({ refreshToken }).catch(() => undefined)`; then clear + navigate.
- [ ] **Step 2:** E2E: assert `POST **/api/auth/logout` is called with refresh body on confirm (route mock).
- [ ] **Step 3:** `yarn build && yarn test && yarn test:e2e e2e/logout.spec.ts` — all green.

---

## Spec coverage

| Spec item | Task |
| --- | --- |
| Single-flight 401 refresh + one retry | 2 |
| Auth skip paths | 1 + 2 |
| Refresh fail → clear; redirect only protected | 1 + 2 |
| Logout best-effort revoke | 3 |
| Unit + Playwright | 2 + 3 |
