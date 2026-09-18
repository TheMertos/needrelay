package com.yagci.needrelay.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SlidingWindowRateLimiter}.
 */
class SlidingWindowRateLimiterTest {

	@Test
	void allowsUpToMaxThenBlocks() {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, 60_000L);
		assertTrue(limiter.tryConsume("a"));
		assertTrue(limiter.tryConsume("a"));
		assertTrue(limiter.tryConsume("a"));
		assertFalse(limiter.tryConsume("a"));
	}

	@Test
	void keysAreIndependent() {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 60_000L);
		assertTrue(limiter.tryConsume("one"));
		assertTrue(limiter.tryConsume("two"));
		assertFalse(limiter.tryConsume("one"));
	}
}
