package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.OrganizationRole;
import jakarta.validation.constraints.NotNull;

/**
 * Changes a member's role within the organization. Organization admin only.
 *
 * @param organizationRole new role for the member
 */
public record UpdateMemberRoleRequest(
		@NotNull OrganizationRole organizationRole
) {
}
