package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Updates the platform-wide default UI language. Platform admin only.
 *
 * @param defaultLanguage new default UI language code
 */
public record UpdateSystemSettingsRequest(
		@NotBlank String defaultLanguage
) {
}
