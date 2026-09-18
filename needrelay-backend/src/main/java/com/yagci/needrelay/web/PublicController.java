package com.yagci.needrelay.web;

import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.security.ClientIpResolver;
import com.yagci.needrelay.security.OfferRateLimiter;
import com.yagci.needrelay.service.NeedService;
import com.yagci.needrelay.service.OfferService;
import com.yagci.needrelay.service.OrganizerContactService;
import com.yagci.needrelay.service.PublicDiscoveryService;
import com.yagci.needrelay.service.ReliefRequestService;
import com.yagci.needrelay.web.dto.CreateOfferRequest;
import com.yagci.needrelay.web.dto.NeedResponse;
import com.yagci.needrelay.web.dto.OfferResponse;
import com.yagci.needrelay.web.dto.OrganizerContactResponse;
import com.yagci.needrelay.web.dto.PublicDiscoveryResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
	 * Returns ACTIVE help points and discoverable needs for the public map/list.
	 *
	 * @return discovery payload
	 */
	@GetMapping("/discovery")
	@Operation(summary = "Public help discovery feed")
	@ApiResponse(responseCode = "200", description = "Discovery payload")
	public PublicDiscoveryResponse discovery() {
		return publicDiscoveryService.getDiscovery();
	}

	/**
	 * Returns a public relief request and its needs by slug.
	 *
	 * @param slug public slug
	 * @return public view
	 */
	@GetMapping("/relief-requests/{slug}")
	@Operation(summary = "Get public relief request by slug")
	@ApiResponse(responseCode = "200", description = "Found")
	public PublicReliefRequestResponse getBySlug(@PathVariable String slug) {
		ReliefRequest request = reliefRequestService.getBySlug(slug);
		Organizer organizer = request.getOrganizer();
		List<NeedResponse> needs = needService.listByRequestId(request.getId());
		List<OrganizerContactResponse> contacts = contactService.list(organizer.getId());
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
				organizer.getDisplayName(),
				organizer.getDescription(),
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
