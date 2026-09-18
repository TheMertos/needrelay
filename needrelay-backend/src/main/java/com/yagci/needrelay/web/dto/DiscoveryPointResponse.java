package com.yagci.needrelay.web.dto;

import java.util.UUID;

/**
 * Map pin for an ACTIVE public relief request.
 *
 * @param id request id
 * @param title title
 * @param locationLabel location label
 * @param latitude latitude
 * @param longitude longitude
 * @param publicSlug public slug
 */
public record DiscoveryPointResponse(
		UUID id,
		String title,
		String locationLabel,
		double latitude,
		double longitude,
		String publicSlug
) {
}
