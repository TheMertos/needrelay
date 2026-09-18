package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create relief request payload.
 *
 * @param title title
 * @param description description
 * @param locationLabel human-readable location
 * @param latitude latitude
 * @param longitude longitude
 */
public record CreateReliefRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank String description,
		@NotBlank @Size(max = 300) String locationLabel,
		@NotNull Double latitude,
		@NotNull Double longitude
) {
}
