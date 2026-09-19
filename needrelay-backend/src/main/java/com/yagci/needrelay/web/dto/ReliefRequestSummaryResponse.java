package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.ReliefRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Relief request list item with dashboard need counts.
 *
 * @param id request id
 * @param title title
 * @param locationLabel location label
 * @param publicSlug public slug
 * @param status status
 * @param openNeeds OPEN + PARTIALLY_COVERED needs
 * @param criticalNeeds critical open/partial needs
 * @param coveredNeeds COVERED needs
 * @param createdAt created at
 * @param updatedAt updated at
 */
public record ReliefRequestSummaryResponse(
		UUID id,
		String title,
		String locationLabel,
		String publicSlug,
		ReliefRequestStatus status,
		long openNeeds,
		long criticalNeeds,
		long coveredNeeds,
		Instant createdAt,
		Instant updatedAt
) {
}
