package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials.
 *
 * @param email organizer email
 * @param password plain password
 */
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password
) {
}
