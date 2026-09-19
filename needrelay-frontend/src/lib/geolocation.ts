export interface GeoCoordinates {
  latitude: number;
  longitude: number;
}

/**
 * Best-effort current position: resolves null on denial, timeout, or when
 * geolocation isn't available, instead of throwing.
 *
 * @param timeoutMs how long to wait before giving up
 * @returns coordinates, or null when unavailable
 */
export function getCurrentPositionSafe(timeoutMs = 6000): Promise<GeoCoordinates | null> {
  return new Promise((resolve) => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      resolve(null);
      return;
    }
    let settled = false;
    const timer = setTimeout(() => {
      if (!settled) {
        settled = true;
        resolve(null);
      }
    }, timeoutMs);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });
      },
      () => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        resolve(null);
      },
      { timeout: timeoutMs, maximumAge: 60000 },
    );
  });
}
