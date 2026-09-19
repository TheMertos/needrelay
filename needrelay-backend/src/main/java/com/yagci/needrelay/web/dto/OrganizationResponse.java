package com.yagci.needrelay.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Organization profile.
 *
 * @param id organization id
 * @param name organization name
 * @param description optional organization description
 * @param active whether the organization's members can sign in
 * @param createdAt creation timestamp
 */
public record OrganizationResponse(
		UUID id,
		String name,
		String description,
		boolean active,
		Instant createdAt
) {
}
