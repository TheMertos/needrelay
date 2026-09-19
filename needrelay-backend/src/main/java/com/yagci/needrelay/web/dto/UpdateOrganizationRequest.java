package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Updates an organization's public profile. Organization admin only.
 *
 * @param name organization name
 * @param description optional organization description
 */
public record UpdateOrganizationRequest(
		@NotBlank @Size(max = 200) String name,
		@Size(max = 5000) String description
) {
}
