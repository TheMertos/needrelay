import { describe, expect, it } from 'vitest';
import { NeedCategory, NeedPriority, NeedStatus } from '../api/generated/models';
import {
  priorityRank,
  sortNeedsByUrgency,
  sortSummariesByUrgency,
  sumSummaryCounts,
} from './urgency';

describe('priorityRank', () => {
  it('orders critical before low', () => {
    expect(priorityRank(NeedPriority.CRITICAL)).toBeLessThan(priorityRank(NeedPriority.LOW));
  });
});

describe('sortNeedsByUrgency', () => {
  it('puts critical with higher remaining first among same priority', () => {
    const sorted = sortNeedsByUrgency([
      {
        id: '1',
        title: 'a',
        category: NeedCategory.SUPPLIES,
        quantityRequired: 10,
        quantityOffered: 0,
        quantityPending: 0,
        remaining: 5,
        unit: 'u',
        priority: NeedPriority.CRITICAL,
        status: NeedStatus.OPEN,
      },
      {
        id: '2',
        title: 'b',
        category: NeedCategory.SUPPLIES,
        quantityRequired: 10,
        quantityOffered: 0,
        quantityPending: 0,
        remaining: 9,
        unit: 'u',
        priority: NeedPriority.CRITICAL,
        status: NeedStatus.OPEN,
      },
      {
        id: '3',
        title: 'c',
        category: NeedCategory.SUPPLIES,
        quantityRequired: 1,
        quantityOffered: 0,
        quantityPending: 0,
        remaining: 1,
        unit: 'u',
        priority: NeedPriority.NORMAL,
        status: NeedStatus.OPEN,
      },
    ]);
    expect(sorted.map((n) => n.id)).toEqual(['2', '1', '3']);
  });
});

describe('sortSummariesByUrgency', () => {
  it('sorts by critical then open', () => {
    const sorted = sortSummariesByUrgency([
      {
        id: 'a',
        title: 'a',
        locationLabel: '',
        publicSlug: 'a',
        openNeeds: 9,
        criticalNeeds: 0,
        coveredNeeds: 0,
      },
      {
        id: 'b',
        title: 'b',
        locationLabel: '',
        publicSlug: 'b',
        openNeeds: 1,
        criticalNeeds: 2,
        coveredNeeds: 0,
      },
    ]);
    expect(sorted.map((s) => s.id)).toEqual(['b', 'a']);
  });
});

describe('sumSummaryCounts', () => {
  it('sums critical open covered', () => {
    expect(
      sumSummaryCounts([
        {
          id: '1',
          title: '',
          locationLabel: '',
          publicSlug: '',
          openNeeds: 2,
          criticalNeeds: 1,
          coveredNeeds: 3,
        },
        {
          id: '2',
          title: '',
          locationLabel: '',
          publicSlug: '',
          openNeeds: 4,
          criticalNeeds: 2,
          coveredNeeds: 1,
        },
      ]),
    ).toEqual({ critical: 3, open: 6, covered: 4 });
  });
});
