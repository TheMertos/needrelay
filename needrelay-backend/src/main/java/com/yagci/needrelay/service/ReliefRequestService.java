package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import com.yagci.needrelay.web.dto.CreateReliefRequest;
import com.yagci.needrelay.web.dto.ReliefRequestResponse;
import com.yagci.needrelay.web.dto.ReliefRequestSummaryResponse;
import com.yagci.needrelay.web.dto.UpdateReliefRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owned relief request CRUD and dashboard summary counts.
 */
@Service
@AllArgsConstructor
public class ReliefRequestService {

	private static final List<NeedStatus> OPEN_STATUSES = List.of(NeedStatus.OPEN, NeedStatus.PARTIALLY_COVERED);

	private final ReliefRequestRepository reliefRequestRepository;
	private final OrganizationRepository organizationRepository;
	private final NeedRepository needRepository;
	private final SlugService slugService;

	/**
	 * Creates a relief request owned by the given organization.
	 *
	 * @param organizationId owner id
	 * @param request create payload
	 * @return created request
	 */
	@Transactional
	public ReliefRequestResponse create(UUID organizationId, CreateReliefRequest request) {
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ApiException("ORGANIZATION_NOT_FOUND", "Organization not found", HttpStatus.NOT_FOUND));
		ReliefRequest entity = new ReliefRequest();
		entity.setOrganization(organization);
		entity.setTitle(request.title().trim());
		entity.setDescription(request.description().trim());
		entity.setLocationLabel(request.locationLabel().trim());
		entity.setLatitude(request.latitude());
		entity.setLongitude(request.longitude());
		entity.setPublicSlug(slugService.createUniqueSlug(request.title()));
		reliefRequestRepository.save(entity);
		return toResponse(entity);
	}

	/**
	 * Updates an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId request id
	 * @param request update payload
	 * @return updated request
	 */
	@Transactional
	public ReliefRequestResponse update(UUID organizerId, UUID requestId, UpdateReliefRequest request) {
		ReliefRequest entity = getOwnedEntity(organizerId, requestId);
		entity.setTitle(request.title().trim());
		entity.setDescription(request.description().trim());
		entity.setLocationLabel(request.locationLabel().trim());
		entity.setLatitude(request.latitude());
		entity.setLongitude(request.longitude());
		entity.setStatus(request.status());
		reliefRequestRepository.save(entity);
		return toResponse(entity);
	}

	/**
	 * Lists the organization's relief requests with need summary counts.
	 *
	 * @param organizationId owner id
	 * @return summary list
	 */
	@Transactional(readOnly = true)
	public List<ReliefRequestSummaryResponse> listMine(UUID organizationId) {
		return reliefRequestRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
				.map(this::toSummary)
				.toList();
	}

	/**
	 * Returns an owned relief request DTO.
	 *
	 * @param organizerId owner id
	 * @param requestId request id
	 * @return request DTO
	 */
	@Transactional(readOnly = true)
	public ReliefRequestResponse getOwned(UUID organizerId, UUID requestId) {
		return toResponse(getOwnedEntity(organizerId, requestId));
	}

	/**
	 * Loads a relief request by public slug.
	 *
	 * @param slug public slug
	 * @return entity
	 */
	@Transactional(readOnly = true)
	public ReliefRequest getBySlug(String slug) {
		return reliefRequestRepository.findByPublicSlug(slug)
				.orElseThrow(() -> new ApiException("RELIEF_NOT_FOUND", "Relief request not found", HttpStatus.NOT_FOUND));
	}

	/**
	 * Loads a relief request owned by the organizer or throws 403/404.
	 *
	 * @param organizerId owner id
	 * @param requestId request id
	 * @return owned entity
	 */
	@Transactional(readOnly = true)
	public ReliefRequest getOwnedEntity(UUID organizerId, UUID requestId) {
		ReliefRequest entity = reliefRequestRepository.findById(requestId)
				.orElseThrow(() -> new ApiException("RELIEF_NOT_FOUND", "Relief request not found", HttpStatus.NOT_FOUND));
		assertOwner(organizerId, entity);
		return entity;
	}

	/**
	 * Counts OPEN + PARTIALLY_COVERED needs for a request.
	 *
	 * @param requestId relief request id
	 * @return open needs count
	 */
	public long countOpenNeeds(UUID requestId) {
		return needRepository.countByReliefRequestIdAndStatus(requestId, NeedStatus.OPEN)
				+ needRepository.countByReliefRequestIdAndStatus(requestId, NeedStatus.PARTIALLY_COVERED);
	}

	/**
	 * Counts critical needs that are still open or partially covered.
	 *
	 * @param requestId relief request id
	 * @return critical open count
	 */
	public long countCriticalNeeds(UUID requestId) {
		return needRepository.countCriticalOpen(requestId, NeedPriority.CRITICAL, OPEN_STATUSES);
	}

	/**
	 * Counts COVERED needs for a request.
	 *
	 * @param requestId relief request id
	 * @return covered count
	 */
	public long countCoveredNeeds(UUID requestId) {
		return needRepository.countByReliefRequestIdAndStatus(requestId, NeedStatus.COVERED);
	}

	/**
	 * Ensures the organization owns the relief request.
	 *
	 * @param organizationId candidate owner
	 * @param entity relief request
	 */
	private void assertOwner(UUID organizationId, ReliefRequest entity) {
		if (!entity.getOrganization().getId().equals(organizationId)) {
			throw new ApiException("FORBIDDEN", "You do not own this relief request", HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Maps entity to full response DTO.
	 *
	 * @param entity relief request
	 * @return response
	 */
	private ReliefRequestResponse toResponse(ReliefRequest entity) {
		return new ReliefRequestResponse(
				entity.getId(),
				entity.getTitle(),
				entity.getDescription(),
				entity.getLocationLabel(),
				entity.getLatitude(),
				entity.getLongitude(),
				entity.getPublicSlug(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

	/**
	 * Maps entity to summary response with dashboard counts.
	 *
	 * @param entity relief request
	 * @return summary
	 */
	private ReliefRequestSummaryResponse toSummary(ReliefRequest entity) {
		UUID id = entity.getId();
		return new ReliefRequestSummaryResponse(
				id,
				entity.getTitle(),
				entity.getLocationLabel(),
				entity.getPublicSlug(),
				entity.getStatus(),
				countOpenNeeds(id),
				countCriticalNeeds(id),
				countCoveredNeeds(id),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
