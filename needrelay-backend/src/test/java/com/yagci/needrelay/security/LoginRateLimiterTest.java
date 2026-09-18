package com.yagci.needrelay.security;

import com.yagci.needrelay.config.NeedRelayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LoginRateLimiter}.
 */
class LoginRateLimiterTest {

	private LoginRateLimiter limiter;

	@BeforeEach
	void setUp() {
		NeedRelayProperties properties = new NeedRelayProperties();
		properties.getLoginRateLimit().setMaxAttempts(5);
		properties.getLoginRateLimit().setWindowSeconds(60);
		limiter = new LoginRateLimiter(properties);
	}

	@Test
	void allowsUpToMaxAttemptsThenBlocks() {
		String ip = "203.0.113.10";
		assertTrue(limiter.tryConsume(ip));
		assertTrue(limiter.tryConsume(ip));
		assertTrue(limiter.tryConsume(ip));
		assertTrue(limiter.tryConsume(ip));
		assertTrue(limiter.tryConsume(ip));
		assertFalse(limiter.tryConsume(ip));
	}

	@Test
	void tracksIpsIndependently() {
		assertTrue(limiter.tryConsume("10.0.0.1"));
		assertTrue(limiter.tryConsume("10.0.0.2"));
	}
}
