import { describe, expect, it } from 'vitest';
import { normalizeOfferPage } from './normalizeOfferPage';

describe('normalizeOfferPage', () => {
  it('accepts paged payload', () => {
    expect(
      normalizeOfferPage({
        items: [],
        page: 1,
        size: 25,
        totalElements: 0,
        totalPages: 0,
      }),
    ).toEqual({
      items: [],
      page: 1,
      size: 25,
      totalElements: 0,
      totalPages: 1,
    });
  });

  it('accepts legacy array payload', () => {
    const offers = [{ id: '1' }] as never;
    expect(normalizeOfferPage(offers, 0, 25).items).toEqual(offers);
  });

  it('treats missing items as empty list', () => {
    expect(normalizeOfferPage(undefined).items).toEqual([]);
  });
});
