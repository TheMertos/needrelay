package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
	 * Lists relief requests for an organization, newest first.
	 *
	 * @param organizationId owner id
	 * @return owned requests
	 */
	List<ReliefRequest> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

	/**
	 * Finds a relief request by public slug with organization loaded.
	 *
	 * @param publicSlug public slug
	 * @return request if present
	 */
	@Query("select r from ReliefRequest r join fetch r.organization where r.publicSlug = :publicSlug")
	Optional<ReliefRequest> findByPublicSlug(@Param("publicSlug") String publicSlug);

	/**
	 * Checks slug uniqueness.
	 *
	 * @param publicSlug candidate slug
	 * @return true if taken
	 */
	boolean existsByPublicSlug(String publicSlug);

	/**
	 * Pages relief requests in the given status, with organization loaded, optionally
	 * matching a free-text query against the request's location/title or any of its
	 * discoverable needs' titles.
	 *
	 * @param status request status
	 * @param q free-text query (blank/null matches everything)
	 * @param needStatuses need statuses considered "discoverable" for the need-title match
	 * @param pageable page and size
	 * @return matching requests, newest first
	 */
	@Query(
			value = """
					select r from ReliefRequest r
					join fetch r.organization
					where r.status = :status
					  and (
					    :q is null or :q = ''
					    or lower(r.locationLabel) like lower(concat('%', :q, '%'))
					    or lower(r.title) like lower(concat('%', :q, '%'))
					    or exists (
					      select 1 from Need n
					      where n.reliefRequest = r
					        and n.status in :needStatuses
					        and lower(n.title) like lower(concat('%', :q, '%'))
					    )
					  )
					order by r.createdAt desc
					""",
			countQuery = """
					select count(r) from ReliefRequest r
					where r.status = :status
					  and (
					    :q is null or :q = ''
					    or lower(r.locationLabel) like lower(concat('%', :q, '%'))
					    or lower(r.title) like lower(concat('%', :q, '%'))
					    or exists (
					      select 1 from Need n
					      where n.reliefRequest = r
					        and n.status in :needStatuses
					        and lower(n.title) like lower(concat('%', :q, '%'))
					    )
					  )
					""")
	Page<ReliefRequest> searchActive(
			@Param("status") ReliefRequestStatus status,
			@Param("q") String q,
			@Param("needStatuses") List<NeedStatus> needStatuses,
			Pageable pageable);
}
