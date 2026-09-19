package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for needs.
 */
public interface NeedRepository extends JpaRepository<Need, UUID> {

	/**
	 * Lists needs for a relief request.
	 *
	 * @param reliefRequestId parent request id
	 * @return needs
	 */
	List<Need> findByReliefRequestIdOrderByPriorityAscCreatedAtAsc(UUID reliefRequestId);

	/**
	 * Loads a need with a pessimistic write lock for concurrent offer updates.
	 *
	 * @param id need id
	 * @return locked need if present
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select n from Need n where n.id = :id")
	Optional<Need> findByIdForUpdate(@Param("id") UUID id);

	/**
	 * Counts needs by status for a relief request.
	 *
	 * @param reliefRequestId parent id
	 * @param status need status
	 * @return count
	 */
	long countByReliefRequestIdAndStatus(UUID reliefRequestId, NeedStatus status);

	/**
	 * Counts critical open/partial needs for a relief request.
	 *
	 * @param reliefRequestId parent id
	 * @param priority critical priority
	 * @param statuses statuses still needing help
	 * @return count
	 */
	@Query("""
			select count(n) from Need n
			where n.reliefRequest.id = :reliefRequestId
			  and n.priority = :priority
			  and n.status in :statuses
			""")
	long countCriticalOpen(
			@Param("reliefRequestId") UUID reliefRequestId,
			@Param("priority") NeedPriority priority,
			@Param("statuses") List<NeedStatus> statuses);

	/**
	 * Lists discoverable needs on ACTIVE requests.
	 *
	 * @param requestStatus ACTIVE
	 * @param needStatuses OPEN and PARTIALLY_COVERED
	 * @return needs with request + organization loaded
	 */
	@Query("""
			select n from Need n
			join fetch n.reliefRequest r
			join fetch r.organization
			where r.status = :requestStatus
			  and n.status in :needStatuses
			order by n.priority asc, n.createdAt asc
			""")
	List<Need> findDiscoverable(
			@Param("requestStatus") ReliefRequestStatus requestStatus,
			@Param("needStatuses") List<NeedStatus> needStatuses);

	/**
	 * Pages discoverable needs for one ACTIVE relief request, most urgent first.
	 *
	 * @param reliefRequestId parent request id
	 * @param needStatuses OPEN and PARTIALLY_COVERED
	 * @param pageable page and size
	 * @return need page with request + organization loaded
	 */
	@Query(value = """
			select n from Need n
			join fetch n.reliefRequest r
			join fetch r.organization
			where r.id = :reliefRequestId
			  and r.status = com.yagci.needrelay.domain.ReliefRequestStatus.ACTIVE
			  and n.status in :needStatuses
			order by case n.priority
					when com.yagci.needrelay.domain.NeedPriority.CRITICAL then 0
					when com.yagci.needrelay.domain.NeedPriority.HIGH then 1
					when com.yagci.needrelay.domain.NeedPriority.NORMAL then 2
					else 3
				end asc,
				n.createdAt asc
			""",
			countQuery = """
			select count(n) from Need n
			join n.reliefRequest r
			where r.id = :reliefRequestId
			  and r.status = com.yagci.needrelay.domain.ReliefRequestStatus.ACTIVE
			  and n.status in :needStatuses
			""")
	Page<Need> findDiscoverableByRequestId(
			@Param("reliefRequestId") UUID reliefRequestId,
			@Param("needStatuses") List<NeedStatus> needStatuses,
			Pageable pageable);
}
