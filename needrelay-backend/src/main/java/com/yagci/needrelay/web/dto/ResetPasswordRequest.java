package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Completes a password reset with the emailed token.
 *
 * @param token raw reset token
 * @param newPassword new password
 */
public record ResetPasswordRequest(
		@NotBlank String token,
		@NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
