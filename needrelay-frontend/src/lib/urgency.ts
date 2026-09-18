import type { NeedResponse, ReliefRequestSummaryResponse } from '../api/generated/models';
import { NeedPriority } from '../api/generated/models';

const RANK: Record<NeedPriority, number> = {
  [NeedPriority.CRITICAL]: 0,
  [NeedPriority.HIGH]: 1,
  [NeedPriority.NORMAL]: 2,
  [NeedPriority.LOW]: 3,
};

/**
 * Maps need priority to a sortable rank (lower = more urgent).
 *
 * @param priority need priority
 * @returns numeric rank
 */
export function priorityRank(priority: NeedPriority): number {
  return RANK[priority];
}

/**
 * Sorts needs by urgency: priority rank ascending, then remaining descending.
 *
 * @param needs need list
 * @returns new sorted array
 */
export function sortNeedsByUrgency(needs: NeedResponse[]): NeedResponse[] {
  return [...needs].sort((a, b) => {
    const byPriority = priorityRank(a.priority) - priorityRank(b.priority);
    if (byPriority !== 0) {
      return byPriority;
    }
    return b.remaining - a.remaining;
  });
}

/**
 * Sorts dashboard summaries by criticalNeeds then openNeeds (descending).
 *
 * @param items summary list
 * @returns new sorted array
 */
export function sortSummariesByUrgency(
  items: ReliefRequestSummaryResponse[],
): ReliefRequestSummaryResponse[] {
  return [...items].sort((a, b) => {
    if (b.criticalNeeds !== a.criticalNeeds) {
      return b.criticalNeeds - a.criticalNeeds;
    }
    return b.openNeeds - a.openNeeds;
  });
}

/**
 * Aggregates KPI totals from relief request summaries.
 *
 * @param items summary list
 * @returns summed critical/open/covered counts
 */
export function sumSummaryCounts(items: ReliefRequestSummaryResponse[]): {
  critical: number;
  open: number;
  covered: number;
} {
  return items.reduce(
    (acc, item) => ({
      critical: acc.critical + item.criticalNeeds,
      open: acc.open + item.openNeeds,
      covered: acc.covered + item.coveredNeeds,
    }),
    { critical: 0, open: 0, covered: 0 },
  );
}
