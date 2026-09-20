package com.yagci.needrelay.web;

import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.CommentService;
import com.yagci.needrelay.web.dto.CommentResponse;
import com.yagci.needrelay.web.dto.CreateCommentRequest;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Registered-organizer notes/comments on owned relief requests.
 */
@RestController
@RequestMapping("/api/relief-requests/{requestId}/comments")
@Tag(name = "comments")
@SecurityRequirement(name = "bearerAuth")
@AllArgsConstructor
public class CommentController {

	private final CommentService commentService;

	/**
	 * Lists comments for an owned relief request.
	 *
	 * @param requestId relief request id
	 * @return comments
	 */
	@GetMapping
	@Operation(summary = "List comments")
	@ApiResponse(responseCode = "200", description = "Comments")
	public List<CommentResponse> list(@PathVariable UUID requestId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return commentService.list(SecurityUtils.requireOrganizationId(principal), requestId);
	}

	/**
	 * Creates a comment on an owned relief request.
	 *
	 * @param requestId relief request id
	 * @param request body
	 * @return created comment
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create comment")
	@ApiResponse(responseCode = "201", description = "Created")
	public CommentResponse create(
			@PathVariable UUID requestId,
			@Valid @RequestBody CreateCommentRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return commentService.create(
				SecurityUtils.requireOrganizationId(principal), principal.getId(), requestId, request);
	}

	/**
	 * Deletes a comment on an owned relief request.
	 *
	 * @param requestId relief request id
	 * @param commentId comment id
	 */
	@DeleteMapping("/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete comment")
	@ApiResponse(responseCode = "204", description = "Deleted")
	public void delete(@PathVariable UUID requestId, @PathVariable UUID commentId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		commentService.delete(SecurityUtils.requireOrganizationId(principal), requestId, commentId);
	}
}
