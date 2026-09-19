package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.OrganizationRole;

import java.time.Instant;
import java.util.UUID;

/**
 * A person who belongs to the current organization.
 *
 * @param id organizer id
 * @param email email
 * @param displayName person's display name
 * @param organizationRole role within the organization
 * @param active whether the account can sign in
 * @param createdAt creation timestamp
 */
public record OrganizationMemberResponse(
		UUID id,
		String email,
		String displayName,
		OrganizationRole organizationRole,
		boolean active,
		Instant createdAt
) {
}
