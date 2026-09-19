package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.OrganizerRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Organizer (person) profile fields for auth and admin views.
 *
 * @param id organizer id
 * @param email email
 * @param displayName person's display name
 * @param role platform role
 * @param active whether the account can sign in
 * @param organizationId organization id, null for platform admins
 * @param organizationName organization name, null for platform admins
 * @param organizationRole role within the organization, null for platform admins
 * @param createdAt creation timestamp
 */
public record OrganizerResponse(
		UUID id,
		String email,
		String displayName,
		OrganizerRole role,
		boolean active,
		UUID organizationId,
		String organizationName,
		OrganizationRole organizationRole,
		Instant createdAt
) {
}
