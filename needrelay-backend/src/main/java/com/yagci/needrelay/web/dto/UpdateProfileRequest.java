package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Updates the current organizer profile.
 *
 * @param displayName organization / display name
 * @param description optional organization description
 */
public record UpdateProfileRequest(
		@NotBlank @Size(max = 200) String displayName,
		@Size(max = 5000) String description
) {
}
