package com.yagci.needrelay.service;

import com.yagci.needrelay.config.NeedRelayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sends transactional email via the Resend HTTP API, in the platform's current default
 * language (see {@link SystemSettingsService}) until per-user language preferences exist.
 */
@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);
	private static final Set<String> RTL_LANGUAGES = Set.of("ar", "fa", "ur");

	private final NeedRelayProperties properties;
	private final MessageSource messageSource;
	private final SystemSettingsService systemSettingsService;
	private final RestClient restClient;

	/**
	 * @param properties Resend and app URL settings
	 * @param messageSource email copy translations
	 * @param systemSettingsService resolves the platform's current default language
	 */
	public EmailService(
			NeedRelayProperties properties,
			MessageSource messageSource,
			SystemSettingsService systemSettingsService) {
		this.properties = properties;
		this.messageSource = messageSource;
		this.systemSettingsService = systemSettingsService;
		this.restClient = RestClient.builder()
				.baseUrl("https://api.resend.com")
				.build();
	}

	/**
	 * Sends an invite email with a registration link, in the platform's default language.
	 *
	 * @param toEmail recipient
	 * @param inviteToken invite token
	 */
	public void sendInvite(String toEmail, String inviteToken) {
		Locale locale = currentLocale();
		String link = properties.getApp().getPublicBaseUrl() + "/register?invite=" + inviteToken;
		send(toEmail, inviteSubject(locale), inviteHtml(locale, link));
	}

	/**
	 * Sends a password-reset email with a reset link, in the platform's default language.
	 *
	 * @param toEmail recipient
	 * @param rawToken opaque reset token
	 */
	public void sendPasswordReset(String toEmail, String rawToken) {
		Locale locale = currentLocale();
		String link = properties.getApp().getPublicBaseUrl() + "/reset-password?token=" + rawToken;
		send(toEmail, resetSubject(locale), resetHtml(locale, link));
	}

	/**
	 * Resolves the platform's current default language as a {@link Locale}.
	 *
	 * @return locale
	 */
	private Locale currentLocale() {
		return Locale.forLanguageTag(systemSettingsService.get().defaultLanguage());
	}

	/**
	 * @param locale email language
	 * @return translated invite email subject
	 */
	String inviteSubject(Locale locale) {
		return messageSource.getMessage("email.invite.subject", null, locale);
	}

	/**
	 * @param locale email language
	 * @param link registration link, shown both as a button and as plain, copyable text
	 *   (some clients strip anchor hrefs, so the raw URL must be visible too)
	 * @return translated invite email HTML body
	 */
	String inviteHtml(Locale locale, String link) {
		String intro = messageSource.getMessage("email.invite.intro", null, locale);
		String cta = messageSource.getMessage("email.invite.cta", null, locale);
		String fallback = messageSource.getMessage("email.invite.fallback", null, locale);
		return wrap(locale, """
				<p>%s</p>
				<p><a href="%s">%s</a></p>
				<p>%s<br/><code>%s</code></p>
				""".formatted(intro, link, cta, fallback, link));
	}

	/**
	 * @param locale email language
	 * @return translated password-reset email subject
	 */
	String resetSubject(Locale locale) {
		return messageSource.getMessage("email.reset.subject", null, locale);
	}

	/**
	 * @param locale email language
	 * @param link reset link
	 * @return translated password-reset email HTML body
	 */
	String resetHtml(Locale locale, String link) {
		String intro = messageSource.getMessage("email.reset.intro", null, locale);
		String cta = messageSource.getMessage("email.reset.cta", null, locale);
		String outro = messageSource.getMessage("email.reset.outro", null, locale);
		return wrap(locale, """
				<p>%s</p>
				<p><a href="%s">%s</a></p>
				<p>%s</p>
				""".formatted(intro, link, cta, outro));
	}

	/**
	 * Wraps email body content with the right text direction for the language.
	 *
	 * @param locale email language
	 * @param bodyHtml inner HTML
	 * @return wrapped HTML
	 */
	private static String wrap(Locale locale, String bodyHtml) {
		String dir = RTL_LANGUAGES.contains(locale.getLanguage()) ? "rtl" : "ltr";
		return "<div dir=\"%s\">%s</div>".formatted(dir, bodyHtml);
	}

	/**
	 * Posts a simple HTML email to Resend.
	 *
	 * @param to recipient
	 * @param subject subject line
	 * @param htmlBody HTML body
	 * @throws IllegalStateException if the API key is not configured or delivery fails,
	 *   so callers never mistake a skipped send for a delivered one
	 */
	void send(String to, String subject, String htmlBody) {
		String apiKey = properties.getResend().getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			log.warn("RESEND_API_KEY not set — cannot send email to {}", to);
			throw new IllegalStateException("RESEND_API_KEY is not configured");
		}
		Map<String, Object> body = Map.of(
				"from", properties.getResend().getFromEmail(),
				"to", List.of(to),
				"subject", subject,
				"html", htmlBody);
		try {
			restClient.post()
					.uri("/emails")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + apiKey)
					.body(body)
					.retrieve()
					.toBodilessEntity();
		}
		catch (Exception ex) {
			log.error("Failed to send email via Resend to {}: {}", to, ex.getMessage());
			throw new IllegalStateException("Failed to send email", ex);
		}
	}
}
