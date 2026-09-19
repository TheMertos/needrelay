package com.yagci.needrelay.web;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.security.ClientIpResolver;
import com.yagci.needrelay.security.OfferRateLimiter;
import com.yagci.needrelay.service.NeedService;
import com.yagci.needrelay.service.OfferService;
import com.yagci.needrelay.service.OrganizerContactService;
import com.yagci.needrelay.service.PublicDiscoveryService;
import com.yagci.needrelay.service.ReliefRequestService;
import com.yagci.needrelay.web.dto.CreateOfferRequest;
import com.yagci.needrelay.web.dto.DiscoveryNeedResponse;
import com.yagci.needrelay.web.dto.DiscoveryPointResponse;
import com.yagci.needrelay.web.dto.NeedResponse;
import com.yagci.needrelay.web.dto.OfferResponse;
import com.yagci.needrelay.web.dto.OrganizerContactResponse;
import com.yagci.needrelay.web.dto.PageResponse;
import com.yagci.needrelay.web.dto.PublicReliefRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unauthenticated public relief and offer endpoints.
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "Public")
public class PublicController {

	private final ReliefRequestService reliefRequestService;
	private final NeedService needService;
	private final OfferService offerService;
	private final OrganizerContactService contactService;
	private final PublicDiscoveryService publicDiscoveryService;
	private final OfferRateLimiter offerRateLimiter;
	private final ClientIpResolver clientIpResolver;

	/**
	 * @param reliefRequestService relief lookup by slug
	 * @param needService need listing
	 * @param offerService public offers
	 * @param contactService organization contacts
	 * @param publicDiscoveryService public discovery feed
	 * @param offerRateLimiter public offer IP rate limiter
	 * @param clientIpResolver client IP resolver
	 */
	public PublicController(
			ReliefRequestService reliefRequestService,
			NeedService needService,
			OfferService offerService,
			OrganizerContactService contactService,
			PublicDiscoveryService publicDiscoveryService,
			OfferRateLimiter offerRateLimiter,
			ClientIpResolver clientIpResolver) {
		this.reliefRequestService = reliefRequestService;
		this.needService = needService;
		this.offerService = offerService;
		this.contactService = contactService;
		this.publicDiscoveryService = publicDiscoveryService;
		this.offerRateLimiter = offerRateLimiter;
		this.clientIpResolver = clientIpResolver;
	}

	/**
	 * Pages ACTIVE help points for the public map/list (max 50 per page), optionally
	 * matching a free-text query against a point's location/title or its needs' titles.
	 *
	 * @param q optional free-text query
	 * @param page zero-based page index
	 * @param size page size (capped at 50)
	 * @return point page
	 */
	@GetMapping("/discovery")
	@Operation(summary = "Public help discovery feed")
	@ApiResponse(responseCode = "200", description = "Discovery payload")
	public PageResponse<DiscoveryPointResponse> discovery(
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return publicDiscoveryService.getDiscovery(q, page, size);
	}

	/**
	 * Pages discoverable needs for one ACTIVE help point (max 20 per page).
	 *
	 * @param requestId relief request id
	 * @param page zero-based page index
	 * @param size page size (capped at 20)
	 * @return needs page
	 */
	@GetMapping("/discovery/points/{requestId}/needs")
	@Operation(summary = "List discoverable needs for a public help point")
	@ApiResponse(responseCode = "200", description = "Needs page")
	public PageResponse<DiscoveryNeedResponse> discoveryPointNeeds(
			@PathVariable UUID requestId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return publicDiscoveryService.getPointNeeds(requestId, page, size);
	}

	/**
	 * Returns a public relief request and its needs by slug. Archived requests no longer
	 * accept offers, so only the title/status/slug are exposed — no needs, location, map
	 * coordinates, or organization/contact details.
	 *
	 * @param slug public slug
	 * @return public view
	 */
	@GetMapping("/relief-requests/{slug}")
	@Operation(summary = "Get public relief request by slug")
	@ApiResponse(responseCode = "200", description = "Found")
	public PublicReliefRequestResponse getBySlug(@PathVariable String slug) {
		ReliefRequest request = reliefRequestService.getBySlug(slug);
		if (request.getStatus() == ReliefRequestStatus.ARCHIVED) {
			return new PublicReliefRequestResponse(
					request.getId(),
					request.getTitle(),
					"",
					"",
					0.0,
					0.0,
					request.getPublicSlug(),
					request.getStatus(),
					List.of(),
					"",
					null,
					List.of(),
					request.getCreatedAt());
		}
		Organization organization = request.getOrganization();
		List<NeedResponse> needs = needService.listByRequestId(request.getId());
		List<OrganizerContactResponse> contacts = new ArrayList<>(
				contactService.listForRequestPublic(request.getId()));
		contacts.addAll(contactService.list(organization.getId()));
		return new PublicReliefRequestResponse(
				request.getId(),
				request.getTitle(),
				request.getDescription(),
				request.getLocationLabel(),
				request.getLatitude(),
				request.getLongitude(),
				request.getPublicSlug(),
				request.getStatus(),
				needs,
				organization.getName(),
				organization.getDescription(),
				contacts,
				request.getCreatedAt());
	}

	/**
	 * Submits a public offer against a need.
	 *
	 * @param needId need id
	 * @param request offer payload
	 * @param httpRequest servlet request for IP rate limiting
	 * @return created offer
	 */
	@PostMapping("/needs/{needId}/offers")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create public offer")
	@ApiResponse(responseCode = "201", description = "Offer created")
	@ApiResponse(responseCode = "429", description = "Too many offers from this IP")
	public OfferResponse createOffer(
			@PathVariable UUID needId,
			@Valid @RequestBody CreateOfferRequest request,
			HttpServletRequest httpRequest) {
		String clientIp = clientIpResolver.resolve(httpRequest);
		if (!offerRateLimiter.tryConsume(clientIp)) {
			throw new ApiException(
					"OFFER_RATE_LIMITED",
					"Too many offers. Please try again later.",
					HttpStatus.TOO_MANY_REQUESTS);
		}
		return offerService.createPublicOffer(needId, request);
	}
}
