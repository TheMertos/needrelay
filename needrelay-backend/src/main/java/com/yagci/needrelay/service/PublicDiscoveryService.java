package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import com.yagci.needrelay.web.dto.DiscoveryNeedResponse;
import com.yagci.needrelay.web.dto.DiscoveryPointResponse;
import com.yagci.needrelay.web.dto.PublicDiscoveryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Builds the unauthenticated public discovery feed.
 */
@Service
public class PublicDiscoveryService {

	private static final List<NeedStatus> DISCOVERABLE = List.of(
			NeedStatus.OPEN,
			NeedStatus.PARTIALLY_COVERED);

	private final ReliefRequestRepository reliefRequestRepository;
	private final NeedRepository needRepository;

	/**
	 * @param reliefRequestRepository relief requests
	 * @param needRepository needs
	 */
	public PublicDiscoveryService(
			ReliefRequestRepository reliefRequestRepository,
			NeedRepository needRepository) {
		this.reliefRequestRepository = reliefRequestRepository;
		this.needRepository = needRepository;
	}

	/**
	 * Returns ACTIVE pins and discoverable needs.
	 *
	 * @return discovery payload
	 */
	@Transactional(readOnly = true)
	public PublicDiscoveryResponse getDiscovery() {
		var points = reliefRequestRepository
				.findByStatusWithOrganizer(ReliefRequestStatus.ACTIVE)
				.stream()
				.map(r -> new DiscoveryPointResponse(
						r.getId(),
						r.getTitle(),
						r.getLocationLabel(),
						r.getLatitude(),
						r.getLongitude(),
						r.getPublicSlug()))
				.toList();

		var needs = needRepository
				.findDiscoverable(ReliefRequestStatus.ACTIVE, DISCOVERABLE)
				.stream()
				.map(n -> {
					var r = n.getReliefRequest();
					return new DiscoveryNeedResponse(
							n.getId(),
							n.getTitle(),
							n.getPriority(),
							n.getStatus(),
							r.getId(),
							r.getPublicSlug(),
							r.getLocationLabel(),
							r.getOrganizer().getDisplayName());
				})
				.toList();

		return new PublicDiscoveryResponse(points, needs);
	}
}
