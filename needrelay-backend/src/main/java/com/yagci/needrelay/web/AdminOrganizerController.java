package com.yagci.needrelay.web;

import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.AdminOrganizerService;
import com.yagci.needrelay.web.dto.OrganizerResponse;
import com.yagci.needrelay.web.dto.UpdateMemberRoleRequest;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only organizer account management.
 */
@RestController
@RequestMapping("/api/admin/organizers")
@Tag(name = "admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
public class AdminOrganizerController {

	private final AdminOrganizerService adminOrganizerService;

	/**
	 * Lists all organizer accounts.
	 *
	 * @return organizers
	 */
	@GetMapping
	@Operation(summary = "List organizers")
	@ApiResponse(responseCode = "200", description = "Organizers")
	public List<OrganizerResponse> list() {
		return adminOrganizerService.listAll();
	}

	/**
	 * Bans an organizer account.
	 *
	 * @param organizerId target id
	 * @return updated organizer
	 */
	@PostMapping("/{organizerId}/ban")
	@Operation(summary = "Ban organizer")
	@ApiResponse(responseCode = "200", description = "Banned")
	public OrganizerResponse ban(@PathVariable UUID organizerId) {
		OrganizerPrincipal admin = SecurityUtils.getCurrentPrincipal();
		return adminOrganizerService.ban(admin.getId(), organizerId);
	}

	/**
	 * Unbans an organizer account.
	 *
	 * @param organizerId target id
	 * @return updated organizer
	 */
	@PostMapping("/{organizerId}/unban")
	@Operation(summary = "Unban organizer")
	@ApiResponse(responseCode = "200", description = "Unbanned")
	public OrganizerResponse unban(@PathVariable UUID organizerId) {
		return adminOrganizerService.unban(organizerId);
	}

	/**
	 * Changes an organization member's role within their organization.
	 *
	 * @param organizerId target organizer
	 * @param request new organization role
	 * @return updated organizer
	 */
	@PatchMapping("/{organizerId}/role")
	@Operation(summary = "Update organization member role")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OrganizerResponse updateOrganizationRole(
			@PathVariable UUID organizerId,
			@Valid @RequestBody UpdateMemberRoleRequest request) {
		return adminOrganizerService.updateOrganizationRole(organizerId, request.organizationRole());
	}
}
