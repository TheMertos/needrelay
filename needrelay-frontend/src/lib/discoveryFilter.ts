import type { DiscoveryNeedResponse } from '../api/generated/models';

export interface DiscoveryFilter {
  requestId?: string | null;
  query?: string;
}

/**
 * Filters discovery needs by optional pin selection and free-text query.
 *
 * @param needs discovery needs
 * @param filter requestId and/or query
 * @returns filtered needs (same order)
 */
export function filterDiscoveryNeeds(
  needs: DiscoveryNeedResponse[],
  filter: DiscoveryFilter,
): DiscoveryNeedResponse[] {
  const q = filter.query?.trim().toLowerCase() ?? '';
  return needs.filter((need) => {
    if (filter.requestId && need.requestId !== filter.requestId) {
      return false;
    }
    if (!q) {
      return true;
    }
    return (
      need.title.toLowerCase().includes(q) ||
      need.locationLabel.toLowerCase().includes(q)
    );
  });
}
