import type { OfferPageResponse, OfferResponse } from '../api/generated/models';

/**
 * Normalizes listOffers API payloads (paged object or legacy array) into a page shape.
 *
 * @param payload API response
 * @param fallbackPage current page index
 * @param fallbackSize page size used in the request
 * @returns stable OfferPageResponse
 */
export function normalizeOfferPage(
  payload: OfferPageResponse | OfferResponse[] | null | undefined,
  fallbackPage = 0,
  fallbackSize = 20,
): OfferPageResponse {
  if (Array.isArray(payload)) {
    return {
      items: payload,
      page: fallbackPage,
      size: fallbackSize,
      totalElements: payload.length,
      totalPages: 1,
    };
  }
  const items = payload?.items ?? [];
  return {
    items,
    page: payload?.page ?? fallbackPage,
    size: payload?.size ?? fallbackSize,
    totalElements: payload?.totalElements ?? items.length,
    totalPages: Math.max(payload?.totalPages ?? 1, 1),
  };
}
