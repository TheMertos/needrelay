package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Forgot-password request.
 *
 * @param email account email
 */
public record ForgotPasswordRequest(
		@NotBlank @Email String email
) {
}
