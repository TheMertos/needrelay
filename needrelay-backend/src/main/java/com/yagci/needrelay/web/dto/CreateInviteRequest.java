package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Create-invite payload.
 *
 * @param email optional email restriction for the invitee
 * @param daysValid validity in days (default 7 when null)
 * @param organizationId target organization; required for a platform admin caller, ignored
 *        (forced to the caller's own organization) for an organization admin caller
 * @param organizationRole role to grant; only honored for a platform admin caller (defaults to
 *        USER), forced to USER for an organization admin caller
 */
public record CreateInviteRequest(
		@Email @Size(max = 320) String email,
		@Min(1) @Max(90) Integer daysValid,
		UUID organizationId,
		OrganizationRole organizationRole
) {
}
