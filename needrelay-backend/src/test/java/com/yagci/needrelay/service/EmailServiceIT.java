package com.yagci.needrelay.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class EmailServiceIT {

	@Autowired
	private EmailService emailService;

	@Test
	void resolvesEnglishInviteCopy() {
		assertThat(emailService.inviteSubject(Locale.ENGLISH)).isEqualTo("Your NeedRelay organizer invite");
		String html = emailService.inviteHtml(Locale.ENGLISH, "https://example.com/register?invite=tok");
		assertThat(html)
				.contains("dir=\"ltr\"")
				.contains("You have been invited to join NeedRelay as an organizer.")
				.contains("https://example.com/register?invite=tok");
	}

	@Test
	void resolvesGermanInviteCopyDistinctFromEnglish() {
		Locale german = Locale.forLanguageTag("de");
		assertThat(emailService.inviteSubject(german)).isEqualTo("Ihre NeedRelay-Einladung als Koordinator");
		assertThat(emailService.inviteSubject(german)).isNotEqualTo(emailService.inviteSubject(Locale.ENGLISH));
	}

	@Test
	void wrapsRightToLeftLanguagesWithRtlDirection() {
		Locale arabic = Locale.forLanguageTag("ar");
		String html = emailService.inviteHtml(arabic, "https://example.com");
		assertThat(html).contains("dir=\"rtl\"");
	}

	@Test
	void resolvesEmailCopyForEverySupportedLanguage() {
		for (String language : SystemSettingsService.SUPPORTED_LANGUAGES) {
			Locale locale = Locale.forLanguageTag(language);
			assertThat(emailService.resetSubject(locale)).isNotBlank();
			assertThat(emailService.resetHtml(locale, "https://example.com/reset-password?token=abc"))
					.contains("https://example.com/reset-password?token=abc");
			assertThat(emailService.inviteSubject(locale)).isNotBlank();
			assertThat(emailService.inviteHtml(locale, "https://example.com/register?invite=abc"))
					.contains("https://example.com/register?invite=abc");
		}
	}

	@Test
	void throwsRatherThanSilentlySkippingWhenNoApiKeyIsConfigured() {
		assertThatThrownBy(() -> emailService.send("someone@example.com", "subject", "<p>body</p>"))
				.isInstanceOf(IllegalStateException.class);
	}
}
