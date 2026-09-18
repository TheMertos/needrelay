package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create comment payload (registered organizers only).
 *
 * @param body comment text
 */
public record CreateCommentRequest(
		@NotBlank @Size(max = 4000) String body
) {
}
