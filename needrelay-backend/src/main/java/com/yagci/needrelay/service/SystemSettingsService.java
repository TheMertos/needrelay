package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.SystemSettings;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.SystemSettingsRepository;
import com.yagci.needrelay.web.dto.SystemSettingsResponse;
import com.yagci.needrelay.web.dto.UpdateSystemSettingsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Platform-wide settings: currently just the default UI language shown to visitors and
 * used for transactional emails until a user picks their own language.
 */
@Service
public class SystemSettingsService {

	/** UI language codes supported by the frontend and email templates. */
	public static final Set<String> SUPPORTED_LANGUAGES = Set.of(
			"en", "de", "ar", "tr", "fr", "es", "pt", "ru", "zh", "ja",
			"hi", "id", "it", "nl", "pl", "uk", "fa", "ur", "ko", "vi");

	private static final int SETTINGS_ID = 1;
	private static final String DEFAULT_LANGUAGE = "en";

	private final SystemSettingsRepository systemSettingsRepository;

	/**
	 * @param systemSettingsRepository settings persistence
	 */
	public SystemSettingsService(SystemSettingsRepository systemSettingsRepository) {
		this.systemSettingsRepository = systemSettingsRepository;
	}

	/**
	 * Returns the current platform-wide default language, "en" if never configured.
	 *
	 * @return settings
	 */
	@Transactional(readOnly = true)
	public SystemSettingsResponse get() {
		return systemSettingsRepository.findById(SETTINGS_ID)
				.map(SystemSettingsService::toResponse)
				.orElseGet(() -> new SystemSettingsResponse(DEFAULT_LANGUAGE, null, null));
	}

	/**
	 * Updates the platform-wide default language. Platform admin only (enforced by caller).
	 *
	 * @param request new default language
	 * @return updated settings
	 */
	@Transactional
	public SystemSettingsResponse update(UpdateSystemSettingsRequest request) {
		String language = request.defaultLanguage().trim().toLowerCase();
		if (!SUPPORTED_LANGUAGES.contains(language)) {
			throw new ApiException("UNSUPPORTED_LANGUAGE", "Unsupported language code", HttpStatus.BAD_REQUEST);
		}
		SystemSettings settings = systemSettingsRepository.findById(SETTINGS_ID).orElseGet(() -> {
			SystemSettings created = new SystemSettings();
			created.setId(SETTINGS_ID);
			// The fixed id means Spring Data JPA treats this as an update rather than an
			// insert, so @PrePersist never runs — set createdAt explicitly here instead.
			created.setCreatedAt(Instant.now());
			return created;
		});
		settings.setDefaultLanguage(language);
		settings.setUpdatedAt(Instant.now());
		systemSettingsRepository.save(settings);
		return toResponse(settings);
	}

	/**
	 * Maps entity to response DTO.
	 *
	 * @param settings entity
	 * @return response
	 */
	private static SystemSettingsResponse toResponse(SystemSettings settings) {
		return new SystemSettingsResponse(settings.getDefaultLanguage(), settings.getCreatedAt(), settings.getUpdatedAt());
	}
}
