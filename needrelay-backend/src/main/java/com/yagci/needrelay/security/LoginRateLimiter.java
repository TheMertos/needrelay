package com.yagci.needrelay.security;

import com.yagci.needrelay.config.NeedRelayProperties;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding-window rate limiter keyed by client IP for login attempts.
 */
@Component
public class LoginRateLimiter {

	private final SlidingWindowRateLimiter delegate;

	/**
	 * @param properties app properties including login rate-limit settings
	 */
	public LoginRateLimiter(NeedRelayProperties properties) {
		NeedRelayProperties.LoginRateLimit rateLimit = properties.getLoginRateLimit();
		this.delegate = new SlidingWindowRateLimiter(
				rateLimit.getMaxAttempts(),
				rateLimit.getWindowSeconds() * 1000L);
	}

	/**
	 * Returns whether another login attempt is allowed for the IP.
	 *
	 * @param clientIp client IP
	 * @return true if under the limit
	 */
	public boolean tryConsume(String clientIp) {
		return delegate.tryConsume(clientIp);
	}
}
