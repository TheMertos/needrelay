/**
 * Auth session helpers for refresh-failure redirects and interceptor skip rules.
 */

let sessionExpiredHandler: (() => void) | null = null;

/**
 * Registers a callback invoked when the session dies on a protected route.
 *
 * @param handler - navigate-to-login callback, or null to clear
 * @returns void
 */
export function setSessionExpiredHandler(handler: (() => void) | null): void {
  sessionExpiredHandler = handler;
}

/**
 * Clears tokens' caller should have cleared already; redirects only on protected paths.
 *
 * @returns void
 */
export function notifySessionExpired(): void {
  if (typeof window === 'undefined') {
    return;
  }
  if (!isProtectedPath(window.location.pathname)) {
    return;
  }
  sessionExpiredHandler?.();
}

/**
 * Returns whether the pathname requires an authenticated organizer session.
 *
 * @param pathname - location pathname (e.g. /dashboard)
 * @returns true when the route is wrapped by RequireAuth
 */
export function isProtectedPath(pathname: string): boolean {
  const p = pathname.replace(/\/+$/, '') || '/';
  if (p === '/dashboard' || p === '/account' || p === '/invites') {
    return true;
  }
  if (p.startsWith('/admin')) {
    return true;
  }
  if (p.startsWith('/requests')) {
    return true;
  }
  return false;
}

/**
 * Returns whether a request URL must not trigger silent token refresh.
 *
 * @param url - request URL (relative or absolute)
 * @returns true for auth endpoints that handle their own 401s
 */
export function isAuthSkipUrl(url: string | undefined): boolean {
  if (!url) {
    return false;
  }
  return /\/api\/auth\/(login|register|refresh|logout|forgot-password|reset-password)(?:\?|$|\/)/.test(
    url,
  );
}
