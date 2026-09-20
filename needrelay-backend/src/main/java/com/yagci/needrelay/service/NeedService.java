package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.OfferStatus;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OfferRepository;
import com.yagci.needrelay.web.dto.CreateNeedRequest;
import com.yagci.needrelay.web.dto.NeedResponse;
import com.yagci.needrelay.web.dto.UpdateNeedRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Need CRUD for owned relief requests.
 */
@Service
@AllArgsConstructor
public class NeedService {

	private final NeedRepository needRepository;
	private final ReliefRequestService reliefRequestService;
	private final OfferRepository offerRepository;

	/**
	 * Creates a need under an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId parent request id
	 * @param request create payload
	 * @return created need
	 */
	@Transactional
	public NeedResponse create(UUID organizerId, UUID requestId, CreateNeedRequest request) {
		ReliefRequest reliefRequest = reliefRequestService.getOwnedEntity(organizerId, requestId);
		Need need = new Need();
		need.setReliefRequest(reliefRequest);
		need.setTitle(request.title().trim());
		need.setDescription(request.description());
		need.setCategory(request.category());
		need.setQuantityRequired(request.quantityRequired());
		need.setQuantityOffered(BigDecimal.ZERO);
		need.setUnit(request.unit().trim());
		need.setPriority(request.priority());
		need.setStatus(NeedStatus.OPEN);
		needRepository.save(need);
		return toResponse(need);
	}

	/**
	 * Updates a need under an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId parent request id
	 * @param needId need id
	 * @param request update payload
	 * @return updated need
	 */
	@Transactional
	public NeedResponse update(UUID organizerId, UUID requestId, UUID needId, UpdateNeedRequest request) {
		Need need = getOwnedNeed(organizerId, requestId, needId);
		if (need.getStatus() == NeedStatus.CLOSED) {
			throw new ApiException("NEED_CLOSED", "Closed needs cannot be updated", HttpStatus.CONFLICT);
		}
		if (request.quantityRequired().compareTo(need.getQuantityOffered()) < 0) {
			throw new ApiException(
					"QUANTITY_BELOW_OFFERED",
					"Required quantity cannot be lower than already offered quantity",
					HttpStatus.CONFLICT);
		}
		need.setTitle(request.title().trim());
		need.setDescription(request.description());
		need.setCategory(request.category());
		need.setQuantityRequired(request.quantityRequired());
		need.setUnit(request.unit().trim());
		need.setPriority(request.priority());
		need.refreshCoverageStatus();
		needRepository.save(need);
		return toResponse(need);
	}

	/**
	 * Marks a need as CLOSED under an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId parent request id
	 * @param needId need id
	 * @return closed need
	 */
	@Transactional
	public NeedResponse close(UUID organizerId, UUID requestId, UUID needId) {
		Need need = getOwnedNeed(organizerId, requestId, needId);
		if (need.getStatus() == NeedStatus.CLOSED) {
			throw new ApiException("NEED_CLOSED", "Need is already closed", HttpStatus.CONFLICT);
		}
		need.setStatus(NeedStatus.CLOSED);
		needRepository.save(need);
		return toResponse(need);
	}

	/**
	 * Lists needs for an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId parent request id
	 * @return needs
	 */
	@Transactional(readOnly = true)
	public List<NeedResponse> listByRequest(UUID organizerId, UUID requestId) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		return needRepository.findByReliefRequestIdOrderByPriorityAscCreatedAtAsc(requestId).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Lists needs for a public relief request (no ownership check).
	 *
	 * @param requestId parent request id
	 * @return needs
	 */
	@Transactional(readOnly = true)
	public List<NeedResponse> listByRequestId(UUID requestId) {
		return needRepository.findByReliefRequestIdOrderByPriorityAscCreatedAtAsc(requestId).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Loads a need belonging to an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId parent request id
	 * @param needId need id
	 * @return need entity
	 */
	private Need getOwnedNeed(UUID organizerId, UUID requestId, UUID needId) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		Need need = needRepository.findById(needId)
				.orElseThrow(() -> new ApiException("NEED_NOT_FOUND", "Need not found", HttpStatus.NOT_FOUND));
		if (!need.getReliefRequest().getId().equals(requestId)) {
			throw new ApiException("NEED_NOT_FOUND", "Need not found on this request", HttpStatus.NOT_FOUND);
		}
		return need;
	}

	/**
	 * Maps a need entity to the response DTO.
	 *
	 * @param need entity
	 * @return response
	 */
	NeedResponse toResponse(Need need) {
		BigDecimal pending = offerRepository.sumExpectedByNeedAndStatuses(
				need.getId(),
				List.of(OfferStatus.PENDING, OfferStatus.COMING));
		if (pending == null) {
			pending = BigDecimal.ZERO;
		}
		return new NeedResponse(
				need.getId(),
				need.getReliefRequest().getId(),
				need.getTitle(),
				need.getDescription(),
				need.getCategory(),
				need.getQuantityRequired(),
				need.getQuantityOffered(),
				pending,
				need.remaining(),
				need.getUnit(),
				need.getPriority(),
				need.getStatus(),
				need.getCreatedAt(),
				need.getUpdatedAt());
	}
}
