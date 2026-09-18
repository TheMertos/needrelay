package com.yagci.needrelay.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Comment on a relief request.
 *
 * @param id comment id
 * @param reliefRequestId parent request
 * @param authorId author organizer id
 * @param authorDisplayName author name
 * @param body text
 * @param createdAt created
 * @param updatedAt updated
 */
public record CommentResponse(
		UUID id,
		UUID reliefRequestId,
		UUID authorId,
		String authorDisplayName,
		String body,
		Instant createdAt,
		Instant updatedAt
) {
}
