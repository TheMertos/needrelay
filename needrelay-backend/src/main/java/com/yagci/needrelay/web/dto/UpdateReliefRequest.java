package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.ReliefRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Update relief request payload.
 *
 * @param title title
 * @param description description
 * @param locationLabel human-readable location
 * @param latitude latitude
 * @param longitude longitude
 * @param status lifecycle status
 */
public record UpdateReliefRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank String description,
		@NotBlank @Size(max = 300) String locationLabel,
		@NotNull Double latitude,
		@NotNull Double longitude,
		@NotNull ReliefRequestStatus status
) {
}
