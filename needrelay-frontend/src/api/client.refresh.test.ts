import Axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  axiosInstance,
  clearTokens,
  customInstance,
  resetRefreshStateForTests,
  setTokens,
} from './client';
import { setSessionExpiredHandler } from './session';

/**
 * Builds a 401 AxiosError for the interceptor under test.
 *
 * @param config - failed request config
 * @returns AxiosError with status 401
 */
function unauthorized(config: InternalAxiosRequestConfig): AxiosError {
  return new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, {
    status: 401,
    statusText: 'Unauthorized',
    data: {},
    headers: {},
    config,
  } as AxiosResponse);
}

describe('auth refresh interceptor', () => {
  beforeEach(() => {
    clearTokens();
    resetRefreshStateForTests();
    setSessionExpiredHandler(null);
    window.history.pushState({}, '', '/');
  });

  afterEach(() => {
    vi.restoreAllMocks();
    clearTokens();
    resetRefreshStateForTests();
    setSessionExpiredHandler(null);
  });

  it('refreshes once and retries the original request', async () => {
    setTokens('old-access', 'refresh-1');
    const postSpy = vi.spyOn(Axios, 'post').mockResolvedValue({
      data: { accessToken: 'new-access', refreshToken: 'refresh-2' },
    });

    let meCalls = 0;
    const adapter = vi.fn(async (config: InternalAxiosRequestConfig) => {
      if (config.url === '/api/auth/me') {
        meCalls += 1;
        if (meCalls === 1) {
          throw unauthorized(config);
        }
        return {
          data: { email: 'a@b.c' },
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
        };
      }
      throw new Error(`unexpected url ${config.url}`);
    });
    axiosInstance.defaults.adapter = adapter;

    const result = await customInstance<{ email: string }>({
      url: '/api/auth/me',
      method: 'GET',
    });

    expect(result.email).toBe('a@b.c');
    expect(postSpy).toHaveBeenCalledTimes(1);
    expect(localStorage.getItem('needrelay.accessToken')).toBe('new-access');
    expect(localStorage.getItem('needrelay.refreshToken')).toBe('refresh-2');
    expect(meCalls).toBe(2);
  });

  it('does not refresh on login 401', async () => {
    setTokens('access', 'refresh-1');
    const postSpy = vi.spyOn(Axios, 'post');

    axiosInstance.defaults.adapter = async (config) => {
      throw unauthorized(config);
    };

    await expect(
      customInstance({ url: '/api/auth/login', method: 'POST', data: {} }),
    ).rejects.toBeTruthy();
    expect(postSpy).not.toHaveBeenCalled();
    expect(localStorage.getItem('needrelay.refreshToken')).toBe('refresh-1');
  });

  it('clears tokens and notifies on protected path when refresh fails', async () => {
    setTokens('old-access', 'bad-refresh');
    window.history.pushState({}, '', '/dashboard');
    const handler = vi.fn();
    setSessionExpiredHandler(handler);
    vi.spyOn(Axios, 'post').mockRejectedValue(new Error('REFRESH_INVALID'));

    axiosInstance.defaults.adapter = async (config) => {
      throw unauthorized(config);
    };

    await expect(
      customInstance({ url: '/api/relief-requests', method: 'GET' }),
    ).rejects.toBeTruthy();

    expect(localStorage.getItem('needrelay.accessToken')).toBeNull();
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('shares one refresh across parallel 401s', async () => {
    setTokens('old-access', 'refresh-1');
    const postSpy = vi.spyOn(Axios, 'post').mockImplementation(async () => {
      await new Promise((r) => setTimeout(r, 20));
      return { data: { accessToken: 'new-access', refreshToken: 'refresh-2' } };
    });

    const counts = new Map<string, number>();
    axiosInstance.defaults.adapter = async (config) => {
      const key = config.url ?? '';
      const n = (counts.get(key) ?? 0) + 1;
      counts.set(key, n);
      if (n === 1) {
        throw unauthorized(config);
      }
      return { data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config };
    };

    await Promise.all([
      customInstance({ url: '/api/a', method: 'GET' }),
      customInstance({ url: '/api/b', method: 'GET' }),
    ]);

    expect(postSpy).toHaveBeenCalledTimes(1);
  });
});
