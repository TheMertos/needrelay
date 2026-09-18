package com.yagci.needrelay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration for JWT, admin bootstrap, CORS, Resend, and app URLs.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "needrelay")
public class NeedRelayProperties {

	private final Jwt jwt = new Jwt();
	private final Admin admin = new Admin();
	private final Cors cors = new Cors();
	private final Resend resend = new Resend();
	private final App app = new App();
	private final LoginRateLimit loginRateLimit = new LoginRateLimit();
	private final OfferRateLimit offerRateLimit = new OfferRateLimit();

	/**
	 * JWT signing and lifetime settings.
	 */
	@Getter
	@Setter
	public static class Jwt {
		private String secret;
		private long accessTokenMinutes = 15;
		private long refreshTokenDays = 14;
	}

	/**
	 * Bootstrap admin credentials used when no organizers exist.
	 */
	@Getter
	@Setter
	public static class Admin {
		private String email;
		private String password;
		private String displayName;
	}

	/**
	 * CORS allowed origins (comma-separated in properties).
	 */
	@Getter
	@Setter
	public static class Cors {
		private String allowedOrigins = "http://localhost:5173";
	}

	/**
	 * Resend transactional email settings.
	 */
	@Getter
	@Setter
	public static class Resend {
		private String apiKey = "";
		private String fromEmail = "NeedRelay <onboarding@resend.dev>";
	}

	/**
	 * Public frontend base URL used in email links.
	 */
	@Getter
	@Setter
	public static class App {
		private String publicBaseUrl = "http://localhost:5173";
	}

	/**
	 * Login brute-force protection (per client IP).
	 */
	@Getter
	@Setter
	public static class LoginRateLimit {
		private int maxAttempts = 5;
		private int windowSeconds = 60;
	}

	/**
	 * Public offer spam protection (per client IP).
	 */
	@Getter
	@Setter
	public static class OfferRateLimit {
		private int maxAttempts = 10;
		private int windowSeconds = 600;
	}
}
