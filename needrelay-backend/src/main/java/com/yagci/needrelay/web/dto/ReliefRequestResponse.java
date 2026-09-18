package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.ReliefRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Full relief request response for owners.
 *
 * @param id request id
 * @param title title
 * @param description description
 * @param locationLabel location label
 * @param latitude latitude
 * @param longitude longitude
 * @param publicSlug public slug
 * @param status status
 * @param createdAt created at
 * @param updatedAt updated at
 */
public record ReliefRequestResponse(
		UUID id,
		String title,
		String description,
		String locationLabel,
		double latitude,
		double longitude,
		String publicSlug,
		ReliefRequestStatus status,
		Instant createdAt,
		Instant updatedAt
) {
}
