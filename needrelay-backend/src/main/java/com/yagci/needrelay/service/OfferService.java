package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.Offer;
import com.yagci.needrelay.domain.OfferStatus;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OfferRepository;
import com.yagci.needrelay.web.dto.CreateOfferRequest;
import com.yagci.needrelay.web.dto.OfferResponse;
import com.yagci.needrelay.web.dto.ReceiveOfferRequest;
import com.yagci.needrelay.web.dto.UpdateOfferRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public offer creation and organizer offer management.
 */
@Service
public class OfferService {

	private static final List<OfferStatus> PENDING_STATUSES = List.of(OfferStatus.PENDING, OfferStatus.COMING);

	private final OfferRepository offerRepository;
	private final NeedRepository needRepository;
	private final ReliefRequestService reliefRequestService;

	/**
	 * @param offerRepository offer persistence
	 * @param needRepository need locking/persistence
	 * @param reliefRequestService ownership checks
	 */
	public OfferService(
			OfferRepository offerRepository,
			NeedRepository needRepository,
			ReliefRequestService reliefRequestService) {
		this.offerRepository = offerRepository;
		this.needRepository = needRepository;
		this.reliefRequestService = reliefRequestService;
	}

	/**
	 * Creates a PENDING public offer (does not change need received totals).
	 *
	 * @param needId target need id
	 * @param dto offer payload
	 * @return created offer
	 */
	@Transactional
	public OfferResponse createPublicOffer(UUID needId, CreateOfferRequest dto) {
		Need need = needRepository.findByIdForUpdate(needId)
				.orElseThrow(() -> new ApiException("NEED_NOT_FOUND", "Need not found", HttpStatus.NOT_FOUND));
		if (need.getStatus() == NeedStatus.CLOSED) {
			throw new ApiException("NEED_CLOSED", "Cannot offer against a closed need", HttpStatus.CONFLICT);
		}
		if (dto.quantity().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ApiException("INVALID_QUANTITY", "Quantity must be positive", HttpStatus.BAD_REQUEST);
		}

		Offer offer = new Offer();
		offer.setNeed(need);
		offer.setProviderName(dto.providerName().trim());
		offer.setQuantity(dto.quantity());
		offer.setStatus(OfferStatus.PENDING);
		offer.setFirstName(dto.firstName().trim());
		offer.setLastName(dto.lastName().trim());
		offer.setPhone(dto.phone().trim());
		offer.setEmail(dto.email().trim().toLowerCase());
		offer.setNote(dto.note());
		offerRepository.save(offer);
		return toResponse(offer);
	}

	/**
	 * Lists offers for an owned relief request, newest first.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @return offers
	 */
	@Transactional(readOnly = true)
	public List<OfferResponse> listOffers(UUID organizerId, UUID requestId) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		return offerRepository.findByNeedReliefRequestIdOrderByCreatedAtDesc(requestId).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Updates contact fields and expected quantity for a non-received offer.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @param offerId offer id
	 * @param dto update payload
	 * @return updated offer
	 */
	@Transactional
	public OfferResponse updateOffer(UUID organizerId, UUID requestId, UUID offerId, UpdateOfferRequest dto) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		Offer offer = requireOfferOnRequest(offerId, requestId);
		if (offer.getStatus() == OfferStatus.RECEIVED) {
			throw new ApiException(
					"OFFER_ALREADY_RECEIVED",
					"Received offers cannot be edited; adjust via receive flow",
					HttpStatus.CONFLICT);
		}
		if (offer.getStatus() == OfferStatus.CANCELLED) {
			throw new ApiException(
					"OFFER_CANCELLED",
					"Cancelled offers cannot be edited",
					HttpStatus.CONFLICT);
		}
		offer.setProviderName(dto.providerName().trim());
		offer.setQuantity(dto.quantity());
		offer.setFirstName(dto.firstName().trim());
		offer.setLastName(dto.lastName().trim());
		offer.setPhone(dto.phone().trim());
		offer.setEmail(dto.email().trim().toLowerCase());
		offer.setNote(dto.note());
		offerRepository.save(offer);
		return toResponse(offer);
	}

	/**
	 * Marks a PENDING offer as COMING.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @param offerId offer id
	 * @return updated offer
	 */
	@Transactional
	public OfferResponse markComing(UUID organizerId, UUID requestId, UUID offerId) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		Offer offer = requireOfferOnRequest(offerId, requestId);
		if (offer.getStatus() != OfferStatus.PENDING) {
			throw new ApiException(
					"INVALID_OFFER_STATUS",
					"Only PENDING offers can be marked coming",
					HttpStatus.CONFLICT);
		}
		offer.setStatus(OfferStatus.COMING);
		offerRepository.save(offer);
		return toResponse(offer);
	}

	/**
	 * Marks an offer RECEIVED with actual quantity and recomputes need coverage.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @param offerId offer id
	 * @param dto received quantity
	 * @return updated offer
	 */
	@Transactional
	public OfferResponse markReceived(
			UUID organizerId,
			UUID requestId,
			UUID offerId,
			ReceiveOfferRequest dto) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		Offer offer = requireOfferOnRequest(offerId, requestId);
		if (offer.getStatus() == OfferStatus.RECEIVED) {
			throw new ApiException(
					"OFFER_ALREADY_RECEIVED",
					"Offer is already received",
					HttpStatus.CONFLICT);
		}
		if (offer.getStatus() == OfferStatus.CANCELLED) {
			throw new ApiException(
					"OFFER_CANCELLED",
					"Cancelled offers cannot be marked received",
					HttpStatus.CONFLICT);
		}
		Need need = needRepository.findByIdForUpdate(offer.getNeed().getId())
				.orElseThrow(() -> new ApiException("NEED_NOT_FOUND", "Need not found", HttpStatus.NOT_FOUND));

		offer.setStatus(OfferStatus.RECEIVED);
		offer.setQuantityReceived(dto.quantityReceived());
		offerRepository.save(offer);
		recomputeNeedReceived(need);
		return toResponse(offer);
	}

	/**
	 * Cancels a PENDING or COMING offer (organizer-only; does not change received totals).
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @param offerId offer id
	 * @return cancelled offer
	 */
	@Transactional
	public OfferResponse cancelOffer(UUID organizerId, UUID requestId, UUID offerId) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		Offer offer = requireOfferOnRequest(offerId, requestId);
		if (offer.getStatus() != OfferStatus.PENDING && offer.getStatus() != OfferStatus.COMING) {
			throw new ApiException(
					"OFFER_NOT_CANCELLABLE",
					"Only PENDING or COMING offers can be cancelled",
					HttpStatus.CONFLICT);
		}
		offer.setStatus(OfferStatus.CANCELLED);
		offerRepository.save(offer);
		return toResponse(offer);
	}

	/**
	 * Deletes an offer and recomputes need totals if it was received.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @param offerId offer id
	 */
	@Transactional
	public void deleteOffer(UUID organizerId, UUID requestId, UUID offerId) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		Offer offer = requireOfferOnRequest(offerId, requestId);
		Need need = needRepository.findByIdForUpdate(offer.getNeed().getId())
				.orElseThrow(() -> new ApiException("NEED_NOT_FOUND", "Need not found", HttpStatus.NOT_FOUND));
		boolean wasReceived = offer.getStatus() == OfferStatus.RECEIVED;
		offerRepository.delete(offer);
		if (wasReceived) {
			recomputeNeedReceived(need);
		}
	}

	/**
	 * Recomputes need.quantityOffered from all RECEIVED offers.
	 *
	 * @param need locked need
	 */
	private void recomputeNeedReceived(Need need) {
		BigDecimal received = offerRepository.sumReceivedByNeed(need.getId());
		need.setQuantityOffered(received != null ? received : BigDecimal.ZERO);
		need.refreshCoverageStatus();
		needRepository.save(need);
	}

	/**
	 * Loads an offer that belongs to the given relief request.
	 *
	 * @param offerId offer id
	 * @param requestId relief request id
	 * @return offer
	 */
	private Offer requireOfferOnRequest(UUID offerId, UUID requestId) {
		Offer offer = offerRepository.findById(offerId)
				.orElseThrow(() -> new ApiException("OFFER_NOT_FOUND", "Offer not found", HttpStatus.NOT_FOUND));
		if (!offer.getNeed().getReliefRequest().getId().equals(requestId)) {
			throw new ApiException("OFFER_NOT_FOUND", "Offer not found on this request", HttpStatus.NOT_FOUND);
		}
		return offer;
	}

	/**
	 * Maps an offer entity to the response DTO.
	 *
	 * @param offer entity
	 * @return response
	 */
	private OfferResponse toResponse(Offer offer) {
		return new OfferResponse(
				offer.getId(),
				offer.getNeed().getId(),
				offer.getProviderName(),
				offer.getQuantity(),
				offer.getQuantityReceived(),
				offer.getStatus(),
				offer.getFirstName(),
				offer.getLastName(),
				offer.getPhone(),
				offer.getEmail(),
				offer.getNote(),
				offer.getCreatedAt());
	}

	/**
	 * Sums pending expected quantity for a need.
	 *
	 * @param needId need id
	 * @return pending sum
	 */
	@Transactional(readOnly = true)
	public BigDecimal pendingQuantityForNeed(UUID needId) {
		BigDecimal pending = offerRepository.sumExpectedByNeedAndStatuses(needId, PENDING_STATUSES);
		return pending != null ? pending : BigDecimal.ZERO;
	}
}
