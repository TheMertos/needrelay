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
 * @param openNeedsCount OPEN + PARTIALLY_COVERED needs
 * @param criticalNeedsCount critical open/partial needs
 * @param coveredNeedsCount COVERED needs
 * @param createdAt created at
 * @param updatedAt updated at
 */
public record ReliefRequestSummaryResponse(
		UUID id,
		String title,
		String locationLabel,
		String publicSlug,
		ReliefRequestStatus status,
		long openNeedsCount,
		long criticalNeedsCount,
		long coveredNeedsCount,
		Instant createdAt,
		Instant updatedAt
) {
}
