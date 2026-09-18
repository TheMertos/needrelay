package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;

import java.util.UUID;

/**
 * Discoverable need row for the public help list.
 *
 * @param id need id
 * @param title title
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
		NeedPriority priority,
		NeedStatus status,
		UUID requestId,
		String publicSlug,
		String locationLabel,
		String organizationName
) {
}
