import Axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import { isAuthSkipUrl, notifySessionExpired } from './session';

const ACCESS_TOKEN_KEY = 'needrelay.accessToken';
const REFRESH_TOKEN_KEY = 'needrelay.refreshToken';

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean };

/** Shared Axios instance (exported for tests). */
export const axiosInstance = Axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '',
});

let refreshPromise: Promise<string> | null = null;

/**
 * Returns the stored access token, if any.
 *
 * @returns JWT access token or null
 */
export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

/**
 * Persists access and refresh tokens.
 *
 * @param accessToken access JWT
 * @param refreshToken refresh token
 * @returns void
 */
export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

/**
 * Returns the stored refresh token, if any.
 *
 * @returns refresh token or null
 */
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

/**
 * Clears stored auth tokens.
 *
 * @returns void
 */
export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

/**
 * Exchanges the stored refresh token for a new token pair (bare Axios, no interceptor).
 *
 * @returns new access token
 */
export async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new Error('NO_REFRESH_TOKEN');
  }
  const baseURL = axiosInstance.defaults.baseURL ?? '';
  const { data } = await Axios.post<{ accessToken: string; refreshToken: string }>(
    `${baseURL}/api/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } },
  );
  setTokens(data.accessToken, data.refreshToken);
  return data.accessToken;
}

/**
 * Starts or joins the in-flight refresh (single-flight).
 *
 * @returns new access token
 */
function getOrStartRefresh(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

/**
 * Resets single-flight state (tests only).
 *
 * @returns void
 */
export function resetRefreshStateForTests(): void {
  refreshPromise = null;
}

axiosInstance.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryConfig | undefined;
    const status = error.response?.status;
    if (
      !config ||
      (status !== 401 && status !== 403) ||
      config._retry ||
      isAuthSkipUrl(config.url)
    ) {
      return Promise.reject(error);
    }

    if (!getRefreshToken()) {
      clearTokens();
      notifySessionExpired();
      return Promise.reject(error);
    }

    try {
      config._retry = true;
      const accessToken = await getOrStartRefresh();
      config.headers.Authorization = `Bearer ${accessToken}`;
      return axiosInstance.request(config);
    } catch (refreshError) {
      clearTokens();
      notifySessionExpired();
      return Promise.reject(refreshError);
    }
  },
);

/**
 * Orval mutator: shared Axios instance with auth header injection and silent refresh.
 *
 * @param config Axios request config
 * @param options optional Axios overrides
 * @returns response data promise
 */
export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  return axiosInstance({ ...config, ...options }).then(({ data }) => data as T);
};

export default customInstance;
