package com.yagci.needrelay.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter keyed by an arbitrary string (e.g. client IP).
 */
public class SlidingWindowRateLimiter {

	private final Map<String, Deque<Long>> attemptsByKey = new ConcurrentHashMap<>();
	private final int maxAttempts;
	private final long windowMillis;

	/**
	 * @param maxAttempts max events allowed in the window
	 * @param windowMillis window length in milliseconds
	 */
	public SlidingWindowRateLimiter(int maxAttempts, long windowMillis) {
		this.maxAttempts = maxAttempts;
		this.windowMillis = windowMillis;
	}

	/**
	 * Returns whether another event is allowed for the key.
	 *
	 * @param key rate-limit key
	 * @return true if under the limit
	 */
	public boolean tryConsume(String key) {
		long now = System.currentTimeMillis();
		Deque<Long> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		synchronized (attempts) {
			prune(attempts, now);
			if (attempts.size() >= maxAttempts) {
				return false;
			}
			attempts.addLast(now);
			return true;
		}
	}

	/**
	 * Removes timestamps older than the configured window.
	 *
	 * @param attempts attempt timestamps
	 * @param now current epoch millis
	 */
	private void prune(Deque<Long> attempts, long now) {
		long cutoff = now - windowMillis;
		Iterator<Long> iterator = attempts.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() < cutoff) {
				iterator.remove();
			} else {
				break;
			}
		}
	}
}
