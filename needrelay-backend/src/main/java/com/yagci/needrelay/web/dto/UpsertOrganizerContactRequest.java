package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creates or updates an organizer contact.
 *
 * @param name full name
 * @param role role or function
 * @param phone phone number
 * @param email email address
 * @param note optional note
 */
public record UpsertOrganizerContactRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 200) String role,
		@NotBlank @Size(max = 80) String phone,
		@NotBlank @Email @Size(max = 320) String email,
		@Size(max = 2000) String note
) {
}
