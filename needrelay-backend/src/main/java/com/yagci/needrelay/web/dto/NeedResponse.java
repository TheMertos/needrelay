package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Need response including received, pending, and remaining quantities.
 *
 * @param id need id
 * @param reliefRequestId parent request id
 * @param title title
 * @param description description
 * @param category category
 * @param quantityRequired required quantity
 * @param quantityOffered received quantity counted toward the need
 * @param quantityPending expected quantity from PENDING+COMING offers
 * @param remaining remaining quantity (never negative)
 * @param unit unit
 * @param priority priority
 * @param status status
 * @param createdAt created at
 * @param updatedAt updated at
 */
public record NeedResponse(
		UUID id,
		UUID reliefRequestId,
		String title,
		String description,
		NeedCategory category,
		BigDecimal quantityRequired,
		BigDecimal quantityOffered,
		BigDecimal quantityPending,
		BigDecimal remaining,
		String unit,
		NeedPriority priority,
		NeedStatus status,
		Instant createdAt,
		Instant updatedAt
) {
}
