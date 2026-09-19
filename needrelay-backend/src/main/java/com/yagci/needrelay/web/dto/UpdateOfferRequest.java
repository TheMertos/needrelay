package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.ProviderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Organizer update of an incoming offer.
 *
 * @param providerName provider name
 * @param providerType whether the offerer is a private person or an organization
 * @param quantity offered quantity
 * @param firstName offerer first name
 * @param lastName offerer last name
 * @param phone offerer phone
 * @param email offerer email
 * @param note optional note
 */
public record UpdateOfferRequest(
		@NotBlank @Size(max = 200) String providerName,
		@NotNull ProviderType providerType,
		@NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Size(max = 80) String phone,
		@NotBlank @Email @Size(max = 320) String email,
		String note
) {
}
