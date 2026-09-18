import { describe, expect, it } from 'vitest';
import { NeedPriority, NeedStatus } from '../api/generated/models';
import type { DiscoveryNeedResponse } from '../api/generated/models';
import { filterDiscoveryNeeds } from './discoveryFilter';

const base: DiscoveryNeedResponse = {
  id: 'n1',
  title: 'Water tanks',
  priority: NeedPriority.CRITICAL,
  status: NeedStatus.OPEN,
  requestId: 'r1',
  publicSlug: 'camp-a',
  locationLabel: 'Aleppo North',
  organizationName: 'Aid Org',
};

describe('filterDiscoveryNeeds', () => {
  it('filters by selected request id', () => {
    const other = { ...base, id: 'n2', requestId: 'r2', title: 'Food' };
    expect(filterDiscoveryNeeds([base, other], { requestId: 'r1' })).toEqual([base]);
  });

  it('filters by free text on title and locationLabel', () => {
    const other = {
      ...base,
      id: 'n2',
      title: 'Blankets',
      locationLabel: 'Damascus',
    };
    expect(filterDiscoveryNeeds([base, other], { query: 'aleppo' })).toEqual([base]);
    expect(filterDiscoveryNeeds([base, other], { query: 'blank' })).toEqual([other]);
  });

  it('combines pin and text filters', () => {
    const samePin = { ...base, id: 'n2', title: 'Food', locationLabel: 'Aleppo North' };
    expect(
      filterDiscoveryNeeds([base, samePin], { requestId: 'r1', query: 'water' }),
    ).toEqual([base]);
  });
});
