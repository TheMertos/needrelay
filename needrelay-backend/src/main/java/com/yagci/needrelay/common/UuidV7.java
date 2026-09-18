package com.yagci.needrelay.common;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Generates time-ordered UUID version 7 identifiers.
 */
public final class UuidV7 {

	private UuidV7() {
	}

	/**
	 * Creates a new UUID v7.
	 *
	 * @return a fresh UUID7
	 */
	public static UUID generate() {
		return UuidCreator.getTimeOrderedEpoch();
	}
}
