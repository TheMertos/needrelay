import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  isAuthSkipUrl,
  isProtectedPath,
  notifySessionExpired,
  setSessionExpiredHandler,
} from './session';

describe('isProtectedPath', () => {
  it('marks organizer routes as protected', () => {
    expect(isProtectedPath('/dashboard')).toBe(true);
    expect(isProtectedPath('/account')).toBe(true);
    expect(isProtectedPath('/invites')).toBe(true);
    expect(isProtectedPath('/admin/organizers')).toBe(true);
    expect(isProtectedPath('/requests/new')).toBe(true);
    expect(isProtectedPath('/requests/abc')).toBe(true);
  });

  it('marks public routes as unprotected', () => {
    expect(isProtectedPath('/')).toBe(false);
    expect(isProtectedPath('/login')).toBe(false);
    expect(isProtectedPath('/register')).toBe(false);
    expect(isProtectedPath('/forgot-password')).toBe(false);
    expect(isProtectedPath('/reset-password')).toBe(false);
    expect(isProtectedPath('/r/camp-a')).toBe(false);
  });
});

describe('isAuthSkipUrl', () => {
  it('skips auth session endpoints', () => {
    expect(isAuthSkipUrl('/api/auth/login')).toBe(true);
    expect(isAuthSkipUrl('/api/auth/refresh')).toBe(true);
    expect(isAuthSkipUrl('http://localhost:8080/api/auth/logout')).toBe(true);
    expect(isAuthSkipUrl('/api/auth/me')).toBe(false);
    expect(isAuthSkipUrl('/api/public/discovery')).toBe(false);
  });
});

describe('notifySessionExpired', () => {
  afterEach(() => {
    setSessionExpiredHandler(null);
  });

  it('invokes handler only on protected paths', () => {
    const handler = vi.fn();
    setSessionExpiredHandler(handler);

    window.history.pushState({}, '', '/');
    notifySessionExpired();
    expect(handler).not.toHaveBeenCalled();

    window.history.pushState({}, '', '/dashboard');
    notifySessionExpired();
    expect(handler).toHaveBeenCalledTimes(1);
  });
});
