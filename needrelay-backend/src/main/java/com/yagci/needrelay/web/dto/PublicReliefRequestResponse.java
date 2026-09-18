package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.ReliefRequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public relief request view including open needs and organization contacts.
 *
 * @param id request id
 * @param title title
 * @param description description
 * @param locationLabel location label
 * @param latitude latitude
 * @param longitude longitude
 * @param publicSlug public slug
 * @param status status
 * @param needs needs list
 * @param organizationName owning organization display name
 * @param organizationDescription owning organization description
 * @param contacts organization contacts
 * @param createdAt created at
 */
public record PublicReliefRequestResponse(
		UUID id,
		String title,
		String description,
		String locationLabel,
		double latitude,
		double longitude,
		String publicSlug,
		ReliefRequestStatus status,
		List<NeedResponse> needs,
		String organizationName,
		String organizationDescription,
		List<OrganizerContactResponse> contacts,
		Instant createdAt
) {
}
