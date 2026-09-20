package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.OfferStatus;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OfferRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import com.yagci.needrelay.web.dto.DiscoveryNeedResponse;
import com.yagci.needrelay.web.dto.DiscoveryPointResponse;
import com.yagci.needrelay.web.dto.PageResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Builds the unauthenticated public discovery feed.
 */
@Service
@AllArgsConstructor
public class PublicDiscoveryService {

	private static final List<NeedStatus> DISCOVERABLE = List.of(
			NeedStatus.OPEN,
			NeedStatus.PARTIALLY_COVERED);
	private static final List<OfferStatus> PENDING_STATUSES = List.of(
			OfferStatus.PENDING,
			OfferStatus.COMING);
	private static final int MAX_POINT_NEEDS_PAGE_SIZE = 20;
	private static final int MAX_POINTS_PAGE_SIZE = 50;

	private final ReliefRequestRepository reliefRequestRepository;
	private final NeedRepository needRepository;
	private final OfferRepository offerRepository;

	/**
	 * Pages ACTIVE help points, optionally matching a free-text query against a point's
	 * location/title or any of its discoverable needs' titles.
	 *
	 * @param q optional free-text query
	 * @param page zero-based page index
	 * @param size requested page size (capped at 50)
	 * @return point page
	 */
	@Transactional(readOnly = true)
	public PageResponse<DiscoveryPointResponse> getDiscovery(String q, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_POINTS_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize);
		Page<ReliefRequest> result = reliefRequestRepository.searchActive(
				ReliefRequestStatus.ACTIVE, q, DISCOVERABLE, pageable);
		List<DiscoveryPointResponse> items = result.getContent().stream()
				.map(r -> new DiscoveryPointResponse(
						r.getId(),
						r.getTitle(),
						r.getLocationLabel(),
						r.getLatitude(),
						r.getLongitude(),
						r.getPublicSlug()))
				.toList();
		return new PageResponse<>(
				items,
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	/**
	 * Pages discoverable needs for one ACTIVE help point, most urgent first.
	 *
	 * @param requestId relief request id
	 * @param page zero-based page index
	 * @param size requested page size (capped at 20)
	 * @return need page, empty when the request is missing/inactive
	 */
	@Transactional(readOnly = true)
	public PageResponse<DiscoveryNeedResponse> getPointNeeds(UUID requestId, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_POINT_NEEDS_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize);
		Page<Need> result = needRepository.findDiscoverableByRequestId(requestId, DISCOVERABLE, pageable);
		List<DiscoveryNeedResponse> items = result.getContent().stream()
				.map(this::toDiscoveryNeedResponse)
				.toList();
		return new PageResponse<>(
				items,
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	/**
	 * Maps a need (with relief request + organizer loaded) to the discovery DTO.
	 *
	 * @param n need entity
	 * @return discovery response
	 */
	private DiscoveryNeedResponse toDiscoveryNeedResponse(Need n) {
		var r = n.getReliefRequest();
		BigDecimal pending = offerRepository.sumExpectedByNeedAndStatuses(n.getId(), PENDING_STATUSES);
		return new DiscoveryNeedResponse(
				n.getId(),
				n.getTitle(),
				n.getCategory(),
				n.getQuantityRequired(),
				n.getQuantityOffered(),
				pending != null ? pending : BigDecimal.ZERO,
				n.remaining(),
				n.getUnit(),
				n.getPriority(),
				n.getStatus(),
				r.getId(),
				r.getPublicSlug(),
				r.getLocationLabel(),
				r.getOrganization().getName());
	}
}
