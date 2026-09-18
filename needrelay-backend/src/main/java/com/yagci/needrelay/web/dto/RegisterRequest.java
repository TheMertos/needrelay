package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Invite-based organizer registration payload.
 *
 * @param inviteToken one-time invite token
 * @param email organizer email
 * @param password plain password
 * @param displayName public display name
 */
public record RegisterRequest(
		@NotBlank String inviteToken,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 128) String password,
		@NotBlank @Size(max = 200) String displayName
) {
}
