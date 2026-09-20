package com.yagci.needrelay.web;

import com.yagci.needrelay.domain.OfferStatus;
import com.yagci.needrelay.domain.ProviderType;
import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.OfferService;
import com.yagci.needrelay.service.OrganizerContactService;
import com.yagci.needrelay.service.ReliefRequestService;
import com.yagci.needrelay.web.dto.CreateReliefRequest;
import com.yagci.needrelay.web.dto.OfferFilter;
import com.yagci.needrelay.web.dto.OfferResponse;
import com.yagci.needrelay.web.dto.OrganizerContactResponse;
import com.yagci.needrelay.web.dto.PageResponse;
import com.yagci.needrelay.web.dto.ReceiveOfferRequest;
import com.yagci.needrelay.web.dto.ReliefRequestResponse;
import com.yagci.needrelay.web.dto.ReliefRequestSummaryResponse;
import com.yagci.needrelay.web.dto.UpdateOfferRequest;
import com.yagci.needrelay.web.dto.UpdateReliefRequest;
import com.yagci.needrelay.web.dto.UpsertOrganizerContactRequest;
import lombok.AllArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Owned relief request endpoints.
 */
@RestController
@RequestMapping("/api/relief-requests")
@Tag(name = "Relief Requests")
@AllArgsConstructor
public class ReliefRequestController {

	private final ReliefRequestService reliefRequestService;
	private final OfferService offerService;
	private final OrganizerContactService contactService;

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
		return reliefRequestService.create(SecurityUtils.requireOrganizationId(principal), request);
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
		return reliefRequestService.listMine(SecurityUtils.requireOrganizationId(principal));
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
		return reliefRequestService.getOwned(SecurityUtils.requireOrganizationId(principal), requestId);
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
		return reliefRequestService.update(SecurityUtils.requireOrganizationId(principal), requestId, request);
	}

	/**
	 * Lists offers for an owned relief request (paginated, max 50 per page) with
	 * combinable per-column filters. Passing {@code needId} scopes the page/filter/sort
	 * to a single need, so each need's table can page independently.
	 *
	 * @param requestId request id
	 * @param page zero-based page index
	 * @param size page size (capped at 50)
	 * @param sort sort token (property,direction)
	 * @param needId optional single-need scope
	 * @param status exact status filter
	 * @param providerType exact provider type filter
	 * @param q free-text search across name/contact fields
	 * @param minQuantity inclusive lower bound on quantity
	 * @param maxQuantity inclusive upper bound on quantity
	 * @param minDistanceKm inclusive lower bound on distance
	 * @param maxDistanceKm inclusive upper bound on distance
	 * @return paginated offers
	 */
	@GetMapping("/{requestId}/offers")
	@Operation(summary = "List offers for relief request")
	@ApiResponse(responseCode = "200", description = "Offers page")
	public PageResponse<OfferResponse> listOffers(
			@PathVariable UUID requestId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) UUID needId,
			@RequestParam(required = false) OfferStatus status,
			@RequestParam(required = false) ProviderType providerType,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) BigDecimal minQuantity,
			@RequestParam(required = false) BigDecimal maxQuantity,
			@RequestParam(required = false) BigDecimal minDistanceKm,
			@RequestParam(required = false) BigDecimal maxDistanceKm) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		OfferFilter filter = new OfferFilter(
				needId, status, providerType, q, minQuantity, maxQuantity, minDistanceKm, maxDistanceKm);
		return offerService.listOffers(SecurityUtils.requireOrganizationId(principal), requestId, page, size, sort, filter);
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
		return offerService.updateOffer(SecurityUtils.requireOrganizationId(principal), requestId, offerId, request);
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
		return offerService.markComing(SecurityUtils.requireOrganizationId(principal), requestId, offerId);
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
		return offerService.markReceived(SecurityUtils.requireOrganizationId(principal), requestId, offerId, request);
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
		return offerService.cancelOffer(SecurityUtils.requireOrganizationId(principal), requestId, offerId);
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
		offerService.deleteOffer(SecurityUtils.requireOrganizationId(principal), requestId, offerId);
	}

	/**
	 * Lists contact persons specific to an owned relief request.
	 *
	 * @param requestId request id
	 * @return contacts
	 */
	@GetMapping("/{requestId}/contacts")
	@Operation(summary = "List relief request contacts")
	@ApiResponse(responseCode = "200", description = "Contacts")
	public List<OrganizerContactResponse> listContacts(@PathVariable UUID requestId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return contactService.listForRequest(SecurityUtils.requireOrganizationId(principal), requestId);
	}

	/**
	 * Creates a contact person specific to an owned relief request.
	 *
	 * @param requestId request id
	 * @param request payload
	 * @return created contact
	 */
	@PostMapping("/{requestId}/contacts")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create relief request contact")
	@ApiResponse(responseCode = "201", description = "Created")
	public OrganizerContactResponse createContact(
			@PathVariable UUID requestId,
			@Valid @RequestBody UpsertOrganizerContactRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return contactService.createForRequest(SecurityUtils.requireOrganizationId(principal), requestId, request);
	}

	/**
	 * Updates a contact person specific to an owned relief request.
	 *
	 * @param requestId request id
	 * @param contactId contact id
	 * @param request payload
	 * @return updated contact
	 */
	@PutMapping("/{requestId}/contacts/{contactId}")
	@Operation(summary = "Update relief request contact")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OrganizerContactResponse updateContact(
			@PathVariable UUID requestId,
			@PathVariable UUID contactId,
			@Valid @RequestBody UpsertOrganizerContactRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return contactService.updateForRequest(SecurityUtils.requireOrganizationId(principal), requestId, contactId, request);
	}

	/**
	 * Deletes a contact person specific to an owned relief request.
	 *
	 * @param requestId request id
	 * @param contactId contact id
	 */
	@DeleteMapping("/{requestId}/contacts/{contactId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete relief request contact")
	@ApiResponse(responseCode = "204", description = "Deleted")
	public void deleteContact(@PathVariable UUID requestId, @PathVariable UUID contactId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		contactService.deleteForRequest(SecurityUtils.requireOrganizationId(principal), requestId, contactId);
	}
}
