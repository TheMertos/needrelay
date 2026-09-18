package com.yagci.needrelay.web.dto;

/**
 * JWT access/refresh pair returned after auth operations.
 *
 * @param accessToken short-lived JWT
 * @param refreshToken opaque refresh token
 * @param expiresInMinutes access token lifetime in minutes
 * @param organizer authenticated organizer summary
 */
public record TokenResponse(
		String accessToken,
		String refreshToken,
		long expiresInMinutes,
		OrganizerResponse organizer
) {
}
