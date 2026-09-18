package com.yagci.needrelay.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Invite listing/create response.
 *
 * @param id invite id
 * @param token invite token
 * @param email optional restricted email
 * @param expiresAt expiry instant
 * @param usedAt when consumed, or null
 * @param createdAt creation instant
 */
public record InviteResponse(
		UUID id,
		String token,
		String email,
		Instant expiresAt,
		Instant usedAt,
		Instant createdAt
) {
}
