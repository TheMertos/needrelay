package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.OrganizerRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Organizer profile fields for auth and admin views.
 *
 * @param id organizer id
 * @param email email
 * @param displayName display name
 * @param description organization description
 * @param role role
 * @param active whether the account can sign in
 * @param createdAt creation timestamp
 */
public record OrganizerResponse(
		UUID id,
		String email,
		String displayName,
		String description,
		OrganizerRole role,
		boolean active,
		Instant createdAt
) {
}
