package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Create-invite payload.
 *
 * @param email optional email restriction for the invitee
 * @param daysValid validity in days (default 7 when null)
 */
public record CreateInviteRequest(
		@Email @Size(max = 320) String email,
		@Min(1) @Max(90) Integer daysValid
) {
}
