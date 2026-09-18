package com.yagci.needrelay.security;

import com.yagci.needrelay.config.NeedRelayProperties;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding-window rate limiter keyed by client IP for public offer creation.
 */
@Component
public class OfferRateLimiter {

	private final SlidingWindowRateLimiter delegate;

	/**
	 * @param properties app properties including offer rate-limit settings
	 */
	public OfferRateLimiter(NeedRelayProperties properties) {
		NeedRelayProperties.OfferRateLimit rateLimit = properties.getOfferRateLimit();
		this.delegate = new SlidingWindowRateLimiter(
				rateLimit.getMaxAttempts(),
				rateLimit.getWindowSeconds() * 1000L);
	}

	/**
	 * Returns whether another public offer is allowed for the IP.
	 *
	 * @param clientIp client IP
	 * @return true if under the limit
	 */
	public boolean tryConsume(String clientIp) {
		return delegate.tryConsume(clientIp);
	}
}
