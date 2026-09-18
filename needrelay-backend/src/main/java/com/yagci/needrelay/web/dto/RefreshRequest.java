package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh-token rotation request.
 *
 * @param refreshToken raw refresh token
 */
public record RefreshRequest(
		@NotBlank String refreshToken
) {
}
