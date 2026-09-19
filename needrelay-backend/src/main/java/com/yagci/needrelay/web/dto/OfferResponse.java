package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.OfferStatus;
import com.yagci.needrelay.domain.ProviderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Offer response for owners and public confirmation.
 *
 * @param id offer id
 * @param needId need id
 * @param providerName provider name
 * @param providerType whether the offerer is a private person or an organization
 * @param quantity expected quantity
 * @param quantityReceived actual received quantity when RECEIVED
 * @param status lifecycle status
 * @param firstName offerer first name
 * @param lastName offerer last name
 * @param phone offerer phone
 * @param email offerer email
 * @param note note
 * @param distanceKm distance from the offerer's shared location to the relief request,
 *                   null when no location was shared
 * @param createdAt created at
 */
public record OfferResponse(
		UUID id,
		UUID needId,
		String providerName,
		ProviderType providerType,
		BigDecimal quantity,
		BigDecimal quantityReceived,
		OfferStatus status,
		String firstName,
		String lastName,
		String phone,
		String email,
		String note,
		BigDecimal distanceKm,
		Instant createdAt
) {
}
