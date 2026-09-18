package com.yagci.needrelay.service;

import com.yagci.needrelay.config.NeedRelayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional email via the Resend HTTP API.
 */
@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final NeedRelayProperties properties;
	private final RestClient restClient;

	/**
	 * @param properties Resend and app URL settings
	 */
	public EmailService(NeedRelayProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.builder()
				.baseUrl("https://api.resend.com")
				.build();
	}

	/**
	 * Sends an invite email with a registration link.
	 *
	 * @param toEmail recipient
	 * @param inviteToken invite token
	 */
	public void sendInvite(String toEmail, String inviteToken) {
		String link = properties.getApp().getPublicBaseUrl() + "/register?invite=" + inviteToken;
		send(
				toEmail,
				"Your NeedRelay organizer invite",
				"""
						<p>You have been invited to join NeedRelay as an organizer.</p>
						<p><a href="%s">Accept invite and create your account</a></p>
						<p>If the link does not work, open NeedRelay and paste this invite token:<br/><code>%s</code></p>
						""".formatted(link, inviteToken));
	}

	/**
	 * Sends a password-reset email with a reset link.
	 *
	 * @param toEmail recipient
	 * @param rawToken opaque reset token
	 */
	public void sendPasswordReset(String toEmail, String rawToken) {
		String link = properties.getApp().getPublicBaseUrl() + "/reset-password?token=" + rawToken;
		send(
				toEmail,
				"Reset your NeedRelay password",
				"""
						<p>We received a request to reset your NeedRelay password.</p>
						<p><a href="%s">Reset password</a></p>
						<p>This link expires soon. If you did not request a reset, you can ignore this email.</p>
						""".formatted(link));
	}

	/**
	 * Posts a simple HTML email to Resend; no-ops when API key is missing.
	 *
	 * @param to recipient
	 * @param subject subject line
	 * @param htmlBody HTML body
	 */
	void send(String to, String subject, String htmlBody) {
		String apiKey = properties.getResend().getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			log.warn("RESEND_API_KEY not set — skipping email to {}", to);
			return;
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
