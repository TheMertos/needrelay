package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Updates the current person's own profile.
 *
 * @param displayName person's display name
 */
public record UpdateProfileRequest(
		@NotBlank @Size(max = 200) String displayName
) {
}
