package com.yagci.needrelay.web.dto;

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
 * @param quantity offered quantity
 * @param firstName offerer first name
 * @param lastName offerer last name
 * @param phone offerer phone
 * @param email offerer email
 * @param note optional note
 */
public record CreateOfferRequest(
		@NotBlank @Size(max = 200) String providerName,
		@NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Size(max = 80) String phone,
		@NotBlank @Email @Size(max = 320) String email,
		String note
) {
}
