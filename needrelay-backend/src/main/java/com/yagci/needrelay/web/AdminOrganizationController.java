package com.yagci.needrelay.web;

import com.yagci.needrelay.service.AdminOrganizerService;
import com.yagci.needrelay.web.dto.CreateOrganizationRequest;
import com.yagci.needrelay.web.dto.OrganizationResponse;
import com.yagci.needrelay.web.dto.UpdateOrganizationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Admin-only organization creation and listing.
 */
@RestController
@RequestMapping("/api/admin/organizations")
@Tag(name = "admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrganizationController {

	private final AdminOrganizerService adminOrganizerService;

	/**
	 * @param adminOrganizerService admin service
	 */
	public AdminOrganizationController(AdminOrganizerService adminOrganizerService) {
		this.adminOrganizerService = adminOrganizerService;
	}

	/**
	 * Creates a new, empty organization. The admin then creates a targeted invite
	 * (organizationId + organizationRole=ADMIN) to seed its first admin.
	 *
	 * @param request name/description
	 * @return created organization
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create organization")
	@ApiResponse(responseCode = "201", description = "Created")
	public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
		return adminOrganizerService.createOrganization(request);
	}

	/**
	 * Lists all organizations.
	 *
	 * @return organizations
	 */
	@GetMapping
	@Operation(summary = "List organizations")
	@ApiResponse(responseCode = "200", description = "Organizations")
	public List<OrganizationResponse> list() {
		return adminOrganizerService.listOrganizations();
	}

	/**
	 * Updates an organization's name/description.
	 *
	 * @param organizationId target organization
	 * @param request name/description
	 * @return updated organization
	 */
	@PutMapping("/{organizationId}")
	@Operation(summary = "Update organization")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OrganizationResponse update(
			@PathVariable UUID organizationId,
			@Valid @RequestBody UpdateOrganizationRequest request) {
		return adminOrganizerService.updateOrganization(organizationId, request);
	}

	/**
	 * Deactivates an organization; its members can no longer sign in.
	 *
	 * @param organizationId target organization
	 * @return updated organization
	 */
	@PostMapping("/{organizationId}/deactivate")
	@Operation(summary = "Deactivate organization")
	@ApiResponse(responseCode = "200", description = "Deactivated")
	public OrganizationResponse deactivate(@PathVariable UUID organizationId) {
		return adminOrganizerService.deactivateOrganization(organizationId);
	}

	/**
	 * Reactivates an organization.
	 *
	 * @param organizationId target organization
	 * @return updated organization
	 */
	@PostMapping("/{organizationId}/activate")
	@Operation(summary = "Activate organization")
	@ApiResponse(responseCode = "200", description = "Activated")
	public OrganizationResponse activate(@PathVariable UUID organizationId) {
		return adminOrganizerService.activateOrganization(organizationId);
	}
}
