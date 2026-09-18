package com.yagci.needrelay.security;

import com.yagci.needrelay.config.NeedRelayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link OfferRateLimiter}.
 */
class OfferRateLimiterTest {

	private OfferRateLimiter limiter;

	@BeforeEach
	void setUp() {
		NeedRelayProperties properties = new NeedRelayProperties();
		properties.getOfferRateLimit().setMaxAttempts(10);
		properties.getOfferRateLimit().setWindowSeconds(600);
		limiter = new OfferRateLimiter(properties);
	}

	@Test
	void allowsUpToMaxAttemptsThenBlocks() {
		String ip = "198.51.100.20";
		for (int i = 0; i < 10; i++) {
			assertTrue(limiter.tryConsume(ip));
		}
		assertFalse(limiter.tryConsume(ip));
	}

	@Test
	void tracksIpsIndependently() {
		assertTrue(limiter.tryConsume("10.0.0.1"));
		assertTrue(limiter.tryConsume("10.0.0.2"));
	}
}
