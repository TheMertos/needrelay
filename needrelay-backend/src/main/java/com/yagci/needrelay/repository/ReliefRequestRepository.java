package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for relief requests.
 */
public interface ReliefRequestRepository extends JpaRepository<ReliefRequest, UUID> {

	/**
	 * Lists relief requests for an organizer, newest first.
	 *
	 * @param organizerId owner id
	 * @return owned requests
	 */
	List<ReliefRequest> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);

	/**
	 * Finds a relief request by public slug with organizer loaded.
	 *
	 * @param publicSlug public slug
	 * @return request if present
	 */
	@Query("select r from ReliefRequest r join fetch r.organizer where r.publicSlug = :publicSlug")
	Optional<ReliefRequest> findByPublicSlug(@Param("publicSlug") String publicSlug);

	/**
	 * Checks slug uniqueness.
	 *
	 * @param publicSlug candidate slug
	 * @return true if taken
	 */
	boolean existsByPublicSlug(String publicSlug);

	/**
	 * Lists ACTIVE relief requests with organizer loaded.
	 *
	 * @param status request status
	 * @return requests
	 */
	@Query("select r from ReliefRequest r join fetch r.organizer where r.status = :status order by r.createdAt desc")
	List<ReliefRequest> findByStatusWithOrganizer(@Param("status") ReliefRequestStatus status);
}
