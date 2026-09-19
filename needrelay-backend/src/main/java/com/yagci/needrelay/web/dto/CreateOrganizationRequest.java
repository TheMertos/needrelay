package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creates a new, empty organization. Platform admin only.
 *
 * @param name organization name
 * @param description optional organization description
 */
public record CreateOrganizationRequest(
		@NotBlank @Size(max = 200) String name,
		@Size(max = 5000) String description
) {
}
