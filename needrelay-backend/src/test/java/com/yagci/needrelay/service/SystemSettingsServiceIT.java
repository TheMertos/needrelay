package com.yagci.needrelay.service;

import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.SystemSettingsRepository;
import com.yagci.needrelay.web.dto.UpdateSystemSettingsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SystemSettingsServiceIT {

	@Autowired
	private SystemSettingsService systemSettingsService;

	@Autowired
	private SystemSettingsRepository systemSettingsRepository;

	@BeforeEach
	void setUp() {
		systemSettingsRepository.deleteAll();
	}

	@Test
	void defaultsToEnglishWhenNeverConfigured() {
		assertThat(systemSettingsService.get().defaultLanguage()).isEqualTo("en");
	}

	@Test
	void updatesAndPersistsTheDefaultLanguage() {
		var updated = systemSettingsService.update(new UpdateSystemSettingsRequest("de"));

		assertThat(updated.defaultLanguage()).isEqualTo("de");
		assertThat(updated.createdAt()).isNotNull();
		assertThat(updated.updatedAt()).isNotNull();
		assertThat(systemSettingsService.get().defaultLanguage()).isEqualTo("de");
	}

	@Test
	void refreshesUpdatedAtOnEachChangeButKeepsCreatedAtStable() {
		var first = systemSettingsService.update(new UpdateSystemSettingsRequest("de"));
		var second = systemSettingsService.update(new UpdateSystemSettingsRequest("fr"));

		assertThat(second.createdAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
				.isEqualTo(first.createdAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
		assertThat(second.updatedAt()).isAfterOrEqualTo(first.updatedAt());
	}

	@Test
	void normalizesCaseAndWhitespace() {
		var updated = systemSettingsService.update(new UpdateSystemSettingsRequest(" DE "));

		assertThat(updated.defaultLanguage()).isEqualTo("de");
	}

	@Test
	void rejectsAnUnsupportedLanguageCode() {
		assertThatThrownBy(() -> systemSettingsService.update(new UpdateSystemSettingsRequest("xx")))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> {
					ApiException apiException = (ApiException) ex;
					assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(apiException.getCode()).isEqualTo("UNSUPPORTED_LANGUAGE");
				});
	}

	@Test
	void everySupportedLanguageIsAccepted() {
		for (String language : SystemSettingsService.SUPPORTED_LANGUAGES) {
			var updated = systemSettingsService.update(new UpdateSystemSettingsRequest(language));
			assertThat(updated.defaultLanguage()).isEqualTo(language);
		}
	}
}
