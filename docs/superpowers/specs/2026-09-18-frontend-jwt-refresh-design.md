# Frontend JWT refresh + logout revoke

**Date:** 2026-09-18  
**Status:** Approved for implementation  
**Scope:** Frontend only (`needrelay-frontend`). Backend auth endpoints already exist.

## Goal

Complete the auth session loop on the client: silent access-token renewal on 401, and logout that revokes the refresh token server-side.

## Backend (already shipped — do not change)

| Endpoint | Behavior |
| --- | --- |
| `POST /api/auth/refresh` | Validates refresh hash, revokes old refresh, returns new access + refresh pair |
| `POST /api/auth/logout` | Revokes refresh if present (204) |
| Login / register | Issue token pair |

Public without auth: login, register, refresh, logout, forgot/reset password (see `SecurityConfig`).

## Frontend today (gaps)

- Tokens stored in `localStorage`; Axios attaches `Authorization`
- Login/register call `setTokens`
- Logout only `clearTokens()` — no revoke API
- No 401 → refresh → retry interceptor

Orval already exposes `authApi.refresh` / `authApi.logout`.

## Decisions

| Topic | Choice |
| --- | --- |
| Interceptor style | Axios response interceptor + **single-flight** refresh |
| Retry | Exactly one retry of the original request after successful refresh |
| Auth paths skipped | Do not attempt refresh on 401 from `/api/auth/login`, `/register`, `/refresh`, `/logout`, `/forgot-password`, `/reset-password` |
| Refresh failure | `clearTokens()`; redirect to `/login` **only** when the current route is protected (organizer / `RequireAuth`). Public pages stay. |
| Logout | Best-effort `POST /api/auth/logout` with stored refresh token, then always clear local tokens and navigate to `/`. Network/revoke errors must not block logout. |
| Navigation on session death | Prefer a small callback/registry (or `window.location` only as last resort) so the interceptor does not import React Router circularly |

## Behavior detail

### Refresh single-flight

1. Request gets 401.
2. If URL is an auth skip path → reject as-is.
3. If no refresh token → `clearTokens()`, optional protected redirect, reject.
4. If a refresh is already in flight → wait on the same promise.
5. Else start refresh; on success `setTokens`; retry original request with new access token (mark retried so a second 401 does not loop).
6. On refresh failure → `clearTokens()`, protected-only redirect, reject.

### Protected routes

Match the same set as `RequireAuth` / organizer routes in `App.tsx` (dashboard, requests, invites, account, admin, etc.). Public: `/`, `/r/:slug`, `/login`, `/register`, `/forgot-password`, `/reset-password`, and similar.

### Logout (AppLayout)

1. Read refresh token (if any).
2. Call `authApi.logout({ refreshToken })` when present; ignore errors.
3. `clearTokens()`; close modal; `navigate('/')`.

## Non-goals

- Proactive refresh-by-expiry timer
- Backend/OpenAPI changes
- Cookie/httpOnly migration
- Mobile shell / table card layout (separate task; tables under `sm` = stacked cards already decided)

## Testing

- **Unit:** interceptor — successful refresh + retry; failed refresh clears tokens; auth skip paths do not refresh; no infinite retry.
- **Playwright:** logout confirm clears session (and preferably hits logout API or leaves user logged out on reload of a protected route). Refresh path may use route mocking if real short-lived JWTs are impractical in e2e.

## Files (expected)

- `src/api/client.ts` — response interceptor + single-flight
- Small helper for “is protected path” / session-expired redirect
- `AppLayout.tsx` — revoke on logout
- Tests under `src/api/` (+ e2e update if needed)
