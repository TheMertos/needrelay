package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.ProviderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Public offer submission payload.
 *
 * @param providerName provider display name
 * @param providerType whether the offerer is a private person or an organization
 * @param quantity offered quantity
 * @param firstName offerer first name
 * @param lastName offerer last name
 * @param phone offerer phone
 * @param email offerer email
 * @param note optional note
 * @param latitude offerer's current latitude, from an optional browser geolocation share;
 *                 used only to compute {@code distanceKm} and never persisted
 * @param longitude offerer's current longitude, see {@code latitude}
 */
public record CreateOfferRequest(
		@NotBlank @Size(max = 200) String providerName,
		@NotNull ProviderType providerType,
		@NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Size(max = 80) String phone,
		@NotBlank @Email @Size(max = 320) String email,
		String note,
		Double latitude,
		Double longitude
) {
}
