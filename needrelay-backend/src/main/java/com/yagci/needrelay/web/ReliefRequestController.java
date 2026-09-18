package com.yagci.needrelay.web;

import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.OfferService;
import com.yagci.needrelay.service.ReliefRequestService;
import com.yagci.needrelay.web.dto.CreateReliefRequest;
import com.yagci.needrelay.web.dto.OfferResponse;
import com.yagci.needrelay.web.dto.ReceiveOfferRequest;
import com.yagci.needrelay.web.dto.ReliefRequestResponse;
import com.yagci.needrelay.web.dto.ReliefRequestSummaryResponse;
import com.yagci.needrelay.web.dto.UpdateOfferRequest;
import com.yagci.needrelay.web.dto.UpdateReliefRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Owned relief request endpoints.
 */
@RestController
@RequestMapping("/api/relief-requests")
@Tag(name = "Relief Requests")
public class ReliefRequestController {

	private final ReliefRequestService reliefRequestService;
	private final OfferService offerService;

	/**
	 * @param reliefRequestService relief request service
	 * @param offerService offer listing
	 */
	public ReliefRequestController(ReliefRequestService reliefRequestService, OfferService offerService) {
		this.reliefRequestService = reliefRequestService;
		this.offerService = offerService;
	}

	/**
	 * Creates a relief request for the current organizer.
	 *
	 * @param request create payload
	 * @return created request
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create relief request")
	@ApiResponse(responseCode = "201", description = "Created")
	public ReliefRequestResponse create(@Valid @RequestBody CreateReliefRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return reliefRequestService.create(principal.getId(), request);
	}

	/**
	 * Lists the current organizer's relief requests with dashboard counts.
	 *
	 * @return summaries
	 */
	@GetMapping
	@Operation(summary = "List my relief requests")
	@ApiResponse(responseCode = "200", description = "List")
	public List<ReliefRequestSummaryResponse> listMine() {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return reliefRequestService.listMine(principal.getId());
	}

	/**
	 * Returns one owned relief request.
	 *
	 * @param requestId request id
	 * @return request
	 */
	@GetMapping("/{requestId}")
	@Operation(summary = "Get owned relief request")
	@ApiResponse(responseCode = "200", description = "Found")
	public ReliefRequestResponse getOwned(@PathVariable UUID requestId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return reliefRequestService.getOwned(principal.getId(), requestId);
	}

	/**
	 * Updates an owned relief request.
	 *
	 * @param requestId request id
	 * @param request update payload
	 * @return updated request
	 */
	@PutMapping("/{requestId}")
	@Operation(summary = "Update relief request")
	@ApiResponse(responseCode = "200", description = "Updated")
	public ReliefRequestResponse update(
			@PathVariable UUID requestId,
			@Valid @RequestBody UpdateReliefRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return reliefRequestService.update(principal.getId(), requestId, request);
	}

	/**
	 * Lists offers for an owned relief request.
	 *
	 * @param requestId request id
	 * @return offers
	 */
	@GetMapping("/{requestId}/offers")
	@Operation(summary = "List offers for relief request")
	@ApiResponse(responseCode = "200", description = "Offers")
	public List<OfferResponse> listOffers(@PathVariable UUID requestId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return offerService.listOffers(principal.getId(), requestId);
	}

	/**
	 * Updates an incoming offer on an owned relief request.
	 *
	 * @param requestId relief request id
	 * @param offerId offer id
	 * @param request update payload
	 * @return updated offer
	 */
	@PutMapping("/{requestId}/offers/{offerId}")
	@Operation(summary = "Update offer")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OfferResponse updateOffer(
			@PathVariable UUID requestId,
			@PathVariable UUID offerId,
			@Valid @RequestBody UpdateOfferRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return offerService.updateOffer(principal.getId(), requestId, offerId, request);
	}

	/**
	 * Marks a pending offer as coming.
	 *
	 * @param requestId relief request id
	 * @param offerId offer id
	 * @return updated offer
	 */
	@PostMapping("/{requestId}/offers/{offerId}/coming")
	@Operation(summary = "Mark offer as coming")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OfferResponse markOfferComing(@PathVariable UUID requestId, @PathVariable UUID offerId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return offerService.markComing(principal.getId(), requestId, offerId);
	}

	/**
	 * Marks an offer as received with actual quantity.
	 *
	 * @param requestId relief request id
	 * @param offerId offer id
	 * @param request received quantity payload
	 * @return updated offer
	 */
	@PostMapping("/{requestId}/offers/{offerId}/received")
	@Operation(summary = "Mark offer as received")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OfferResponse markOfferReceived(
			@PathVariable UUID requestId,
			@PathVariable UUID offerId,
			@Valid @RequestBody ReceiveOfferRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return offerService.markReceived(principal.getId(), requestId, offerId, request);
	}

	/**
	 * Cancels a pending or coming offer on an owned relief request.
	 *
	 * @param requestId relief request id
	 * @param offerId offer id
	 * @return cancelled offer
	 */
	@PostMapping("/{requestId}/offers/{offerId}/cancel")
	@Operation(summary = "Cancel offer")
	@ApiResponse(responseCode = "200", description = "Cancelled")
	public OfferResponse cancelOffer(@PathVariable UUID requestId, @PathVariable UUID offerId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return offerService.cancelOffer(principal.getId(), requestId, offerId);
	}

	/**
	 * Deletes an incoming offer on an owned relief request.
	 *
	 * @param requestId relief request id
	 * @param offerId offer id
	 */
	@DeleteMapping("/{requestId}/offers/{offerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete offer")
	@ApiResponse(responseCode = "204", description = "Deleted")
	public void deleteOffer(@PathVariable UUID requestId, @PathVariable UUID offerId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		offerService.deleteOffer(principal.getId(), requestId, offerId);
	}
}
