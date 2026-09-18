package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.ReliefRequestComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence for relief-request comments.
 */
public interface ReliefRequestCommentRepository extends JpaRepository<ReliefRequestComment, UUID> {

	/**
	 * Lists comments for a relief request, oldest first.
	 *
	 * @param reliefRequestId parent id
	 * @return comments
	 */
	List<ReliefRequestComment> findByReliefRequestIdOrderByCreatedAtAsc(UUID reliefRequestId);
}
