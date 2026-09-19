package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Discoverable need row for the public help list.
 *
 * @param id need id
 * @param title title
 * @param category category
 * @param quantityRequired total quantity required
 * @param quantityOffered quantity already received
 * @param quantityPending expected quantity from PENDING+COMING offers
 * @param remaining quantity still needed
 * @param unit unit of measure
 * @param priority priority
 * @param status status
 * @param requestId parent request id
 * @param publicSlug parent public slug
 * @param locationLabel parent location label
 * @param organizationName owning organization display name
 */
public record DiscoveryNeedResponse(
		UUID id,
		String title,
		NeedCategory category,
		BigDecimal quantityRequired,
		BigDecimal quantityOffered,
		BigDecimal quantityPending,
		BigDecimal remaining,
		String unit,
		NeedPriority priority,
		NeedStatus status,
		UUID requestId,
		String publicSlug,
		String locationLabel,
		String organizationName
) {
}
