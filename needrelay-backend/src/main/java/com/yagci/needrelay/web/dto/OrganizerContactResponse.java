package com.yagci.needrelay.web.dto;

import java.util.UUID;

/**
 * Organization contact person.
 *
 * @param id contact id
 * @param name full name
 * @param role role or function
 * @param phone phone number
 * @param email email address
 * @param note optional availability note
 * @param sortOrder display order
 */
public record OrganizerContactResponse(
		UUID id,
		String name,
		String role,
		String phone,
		String email,
		String note,
		int sortOrder
) {
}
