package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.Offer;
import com.yagci.needrelay.domain.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Persistence access for offers.
 */
public interface OfferRepository extends JpaRepository<Offer, UUID>, JpaSpecificationExecutor<Offer> {

	/**
	 * Lists offers for a need, newest first.
	 *
	 * @param needId need id
	 * @return offers
	 */
	List<Offer> findByNeedIdOrderByCreatedAtDesc(UUID needId);

	/**
	 * Lists offers for all needs of a relief request, newest first.
	 *
	 * @param reliefRequestId parent request id
	 * @return offers
	 */
	List<Offer> findByNeedReliefRequestIdOrderByCreatedAtDesc(UUID reliefRequestId);

	/**
	 * Pages offers for all needs of a relief request.
	 *
	 * @param reliefRequestId parent request id
	 * @param pageable page and sort
	 * @return offer page
	 */
	Page<Offer> findByNeedReliefRequestId(UUID reliefRequestId, Pageable pageable);

	/**
	 * Sums expected quantities for pending/coming offers on a need.
	 *
	 * @param needId need id
	 * @param statuses PENDING and COMING
	 * @return sum or zero
	 */
	@Query("""
			select coalesce(sum(o.quantity), 0) from Offer o
			where o.need.id = :needId and o.status in :statuses
			""")
	BigDecimal sumExpectedByNeedAndStatuses(
			@Param("needId") UUID needId,
			@Param("statuses") List<OfferStatus> statuses);

	/**
	 * Sums received quantities for RECEIVED offers on a need.
	 *
	 * @param needId need id
	 * @return sum or zero
	 */
	@Query("""
			select coalesce(sum(o.quantityReceived), 0) from Offer o
			where o.need.id = :needId and o.status = com.yagci.needrelay.domain.OfferStatus.RECEIVED
			""")
	BigDecimal sumReceivedByNeed(@Param("needId") UUID needId);
}
