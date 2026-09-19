package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestComment;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.ReliefRequestCommentRepository;
import com.yagci.needrelay.web.dto.CommentResponse;
import com.yagci.needrelay.web.dto.CreateCommentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Notes/comments on relief requests for registered organizers.
 */
@Service
public class CommentService {

	private final ReliefRequestCommentRepository commentRepository;
	private final OrganizerRepository organizerRepository;
	private final ReliefRequestService reliefRequestService;

	/**
	 * @param commentRepository comments
	 * @param organizerRepository authors
	 * @param reliefRequestService ownership
	 */
	public CommentService(
			ReliefRequestCommentRepository commentRepository,
			OrganizerRepository organizerRepository,
			ReliefRequestService reliefRequestService) {
		this.commentRepository = commentRepository;
		this.organizerRepository = organizerRepository;
		this.reliefRequestService = reliefRequestService;
	}

	/**
	 * Lists comments for an owned relief request.
	 *
	 * @param organizationId owner id
	 * @param requestId relief request id
	 * @return comments oldest-first
	 */
	@Transactional(readOnly = true)
	public List<CommentResponse> list(UUID organizationId, UUID requestId) {
		reliefRequestService.getOwnedEntity(organizationId, requestId);
		return commentRepository.findByReliefRequestIdOrderByCreatedAtAsc(requestId).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Adds a comment as the given author on an owned relief request.
	 *
	 * @param organizationId owner id (of the relief request)
	 * @param authorId id of the person posting the comment
	 * @param requestId relief request id
	 * @param request body
	 * @return created comment
	 */
	@Transactional
	public CommentResponse create(UUID organizationId, UUID authorId, UUID requestId, CreateCommentRequest request) {
		ReliefRequest reliefRequest = reliefRequestService.getOwnedEntity(organizationId, requestId);
		Organizer author = organizerRepository.findById(authorId)
				.orElseThrow(() -> new ApiException("ORGANIZER_NOT_FOUND", "Organizer not found", HttpStatus.NOT_FOUND));
		ReliefRequestComment comment = new ReliefRequestComment();
		comment.setReliefRequest(reliefRequest);
		comment.setAuthor(author);
		comment.setBody(request.body().trim());
		commentRepository.save(comment);
		return toResponse(comment);
	}

	/**
	 * Deletes a comment on an owned relief request (owner only).
	 *
	 * @param organizationId owner id
	 * @param requestId relief request id
	 * @param commentId comment id
	 */
	@Transactional
	public void delete(UUID organizationId, UUID requestId, UUID commentId) {
		reliefRequestService.getOwnedEntity(organizationId, requestId);
		ReliefRequestComment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new ApiException("COMMENT_NOT_FOUND", "Comment not found", HttpStatus.NOT_FOUND));
		if (!comment.getReliefRequest().getId().equals(requestId)) {
			throw new ApiException("COMMENT_NOT_FOUND", "Comment not found on this request", HttpStatus.NOT_FOUND);
		}
		commentRepository.delete(comment);
	}

	/**
	 * Maps comment entity to response.
	 *
	 * @param comment entity
	 * @return response
	 */
	private CommentResponse toResponse(ReliefRequestComment comment) {
		return new CommentResponse(
				comment.getId(),
				comment.getReliefRequest().getId(),
				comment.getAuthor().getId(),
				comment.getAuthor().getDisplayName(),
				comment.getBody(),
				comment.getCreatedAt(),
				comment.getUpdatedAt());
	}
}
