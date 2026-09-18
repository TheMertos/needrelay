package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Logout request that revokes a refresh token.
 *
 * @param refreshToken raw refresh token to revoke
 */
public record LogoutRequest(
		@NotBlank String refreshToken
) {
}
