package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.OrganizationRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Invite listing/create response.
 *
 * @param id invite id
 * @param token invite token
 * @param email optional restricted email
 * @param organizationId target organization id
 * @param organizationName target organization name
 * @param organizationRole role the invitee will be granted
 * @param expiresAt expiry instant
 * @param usedAt when consumed, or null
 * @param createdAt creation instant
 * @param emailSentAt when the invite email was delivered, or null if none was queued,
 *   it's still pending, or delivery has failed so far
 * @param emailError error from the most recent failed delivery attempt, or null
 */
public record InviteResponse(
		UUID id,
		String token,
		String email,
		UUID organizationId,
		String organizationName,
		OrganizationRole organizationRole,
		Instant expiresAt,
		Instant usedAt,
		Instant createdAt,
		Instant emailSentAt,
		String emailError
) {
}
