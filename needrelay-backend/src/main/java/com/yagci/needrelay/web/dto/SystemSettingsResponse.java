package com.yagci.needrelay.web.dto;

import java.time.Instant;

/**
 * Platform-wide settings visible to any client, including anonymous visitors, so the UI
 * can boot in the configured default language.
 *
 * @param defaultLanguage default UI language code (e.g. "en")
 * @param createdAt when the settings row was first created
 * @param updatedAt when the settings were last changed
 */
public record SystemSettingsResponse(String defaultLanguage, Instant createdAt, Instant updatedAt) {
}
