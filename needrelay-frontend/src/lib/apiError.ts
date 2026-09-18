import Axios from 'axios';

/**
 * Extracts the machine-readable API error code from an Axios/ProblemDetail error.
 *
 * @param error caught error value
 * @returns API code string or null
 */
export function getApiErrorCode(error: unknown): string | null {
  if (!Axios.isAxiosError(error)) {
    return null;
  }
  const data = error.response?.data;
  if (data && typeof data === 'object') {
    const code = (data as { code?: unknown }).code;
    if (typeof code === 'string') {
      return code;
    }
  }
  if (error.response?.status === 429) {
    return 'RATE_LIMITED';
  }
  return null;
}
