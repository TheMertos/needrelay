package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.OfferStatus;
import com.yagci.needrelay.domain.ProviderType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Combinable offer filters for the organizer offers table.
 *
 * @param needId exact need scope, for a single need's independent table
 * @param status exact lifecycle status match
 * @param providerType exact provider type match
 * @param q free-text search across provider/first/last name, phone and email
 * @param minQuantity inclusive lower bound on expected quantity
 * @param maxQuantity inclusive upper bound on expected quantity
 * @param minDistanceKm inclusive lower bound on distance, offers with no distance excluded
 * @param maxDistanceKm inclusive upper bound on distance, offers with no distance excluded
 */
public record OfferFilter(
		UUID needId,
		OfferStatus status,
		ProviderType providerType,
		String q,
		BigDecimal minQuantity,
		BigDecimal maxQuantity,
		BigDecimal minDistanceKm,
		BigDecimal maxDistanceKm
) {
}
