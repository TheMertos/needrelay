package com.yagci.needrelay.web;

import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.NeedService;
import com.yagci.needrelay.web.dto.CreateNeedRequest;
import com.yagci.needrelay.web.dto.NeedResponse;
import com.yagci.needrelay.web.dto.UpdateNeedRequest;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * Nested need endpoints under a relief request.
 */
@RestController
@RequestMapping("/api/relief-requests/{requestId}/needs")
@Tag(name = "Needs")
@AllArgsConstructor
public class NeedController {

	private final NeedService needService;

	/**
	 * Creates a need under an owned relief request.
	 *
	 * @param requestId parent request id
	 * @param request create payload
	 * @return created need
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create need")
	@ApiResponse(responseCode = "201", description = "Created")
	public NeedResponse create(
			@PathVariable UUID requestId,
			@Valid @RequestBody CreateNeedRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return needService.create(SecurityUtils.requireOrganizationId(principal), requestId, request);
	}

	/**
	 * Lists needs for an owned relief request.
	 *
	 * @param requestId parent request id
	 * @return needs
	 */
	@GetMapping
	@Operation(summary = "List needs")
	@ApiResponse(responseCode = "200", description = "Needs")
	public List<NeedResponse> list(@PathVariable UUID requestId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return needService.listByRequest(SecurityUtils.requireOrganizationId(principal), requestId);
	}

	/**
	 * Updates a need under an owned relief request.
	 *
	 * @param requestId parent request id
	 * @param needId need id
	 * @param request update payload
	 * @return updated need
	 */
	@PutMapping("/{needId}")
	@Operation(summary = "Update need")
	@ApiResponse(responseCode = "200", description = "Updated")
	public NeedResponse update(
			@PathVariable UUID requestId,
			@PathVariable UUID needId,
			@Valid @RequestBody UpdateNeedRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return needService.update(SecurityUtils.requireOrganizationId(principal), requestId, needId, request);
	}

	/**
	 * Closes a need under an owned relief request.
	 *
	 * @param requestId parent request id
	 * @param needId need id
	 * @return closed need
	 */
	@PatchMapping("/{needId}/close")
	@Operation(summary = "Close need")
	@ApiResponse(responseCode = "200", description = "Closed")
	public NeedResponse close(@PathVariable UUID requestId, @PathVariable UUID needId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return needService.close(SecurityUtils.requireOrganizationId(principal), requestId, needId);
	}
}
