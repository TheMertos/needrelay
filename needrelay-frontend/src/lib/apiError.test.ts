import { describe, expect, it } from 'vitest';
import Axios from 'axios';
import { getApiErrorCode } from './apiError';

describe('getApiErrorCode', () => {
  it('reads code from axios problem detail', () => {
    const error = new Axios.AxiosError(
      'Unauthorized',
      'ERR_BAD_REQUEST',
      undefined,
      undefined,
      {
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config: {} as never,
        data: { code: 'INVALID_CREDENTIALS', detail: 'Invalid email or password' },
      },
    );
    expect(getApiErrorCode(error)).toBe('INVALID_CREDENTIALS');
  });

  it('returns null for non-axios errors', () => {
    expect(getApiErrorCode(new Error('boom'))).toBeNull();
  });

  it('maps HTTP 429 without code to RATE_LIMITED', () => {
    const error = new Axios.AxiosError(
      'Too Many Requests',
      'ERR_BAD_REQUEST',
      undefined,
      undefined,
      {
        status: 429,
        statusText: 'Too Many Requests',
        headers: {},
        config: {} as never,
        data: {},
      },
    );
    expect(getApiErrorCode(error)).toBe('RATE_LIMITED');
  });
});
