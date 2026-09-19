package com.yagci.needrelay.web;

import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.OrganizationService;
import com.yagci.needrelay.web.dto.OrganizationMemberResponse;
import com.yagci.needrelay.web.dto.OrganizationResponse;
import com.yagci.needrelay.web.dto.UpdateMemberRoleRequest;
import com.yagci.needrelay.web.dto.UpdateOrganizationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Current organization profile and member management.
 */
@RestController
@RequestMapping("/api/me/organization")
@Tag(name = "organization")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

	private final OrganizationService organizationService;

	/**
	 * @param organizationService organization service
	 */
	public OrganizationController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	/**
	 * Returns the current organization's profile.
	 *
	 * @return profile
	 */
	@GetMapping
	@Operation(summary = "Get my organization")
	@ApiResponse(responseCode = "200", description = "Found")
	public OrganizationResponse get() {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return organizationService.get(SecurityUtils.requireOrganizationId(principal));
	}

	/**
	 * Updates the organization's name/description. Organization admin only.
	 *
	 * @param request payload
	 * @return updated profile
	 */
	@PutMapping
	@Operation(summary = "Update my organization")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OrganizationResponse update(@Valid @RequestBody UpdateOrganizationRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		requireOrgAdmin(principal);
		return organizationService.update(SecurityUtils.requireOrganizationId(principal), request);
	}

	/**
	 * Lists members of the current organization.
	 *
	 * @return members
	 */
	@GetMapping("/members")
	@Operation(summary = "List organization members")
	@ApiResponse(responseCode = "200", description = "Members")
	public List<OrganizationMemberResponse> listMembers() {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return organizationService.listMembers(SecurityUtils.requireOrganizationId(principal));
	}

	/**
	 * Changes a member's role. Organization admin only.
	 *
	 * @param organizerId member to update
	 * @param request new role
	 * @return updated member
	 */
	@PatchMapping("/members/{organizerId}")
	@Operation(summary = "Update member role")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OrganizationMemberResponse updateMemberRole(
			@PathVariable UUID organizerId,
			@Valid @RequestBody UpdateMemberRoleRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		requireOrgAdmin(principal);
		return organizationService.updateMemberRole(
				SecurityUtils.requireOrganizationId(principal), organizerId, request);
	}

	/**
	 * Removes (deactivates) a member. Organization admin only.
	 *
	 * @param organizerId member to remove
	 */
	@DeleteMapping("/members/{organizerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Remove member")
	@ApiResponse(responseCode = "204", description = "Removed")
	public void removeMember(@PathVariable UUID organizerId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		requireOrgAdmin(principal);
		organizationService.removeMember(SecurityUtils.requireOrganizationId(principal), principal.getId(), organizerId);
	}

	/**
	 * Rejects the request unless the caller is an organization admin.
	 *
	 * @param principal current principal
	 */
	private static void requireOrgAdmin(OrganizerPrincipal principal) {
		if (principal.getOrganizationRole() != OrganizationRole.ADMIN) {
			throw new ApiException("FORBIDDEN", "Only organization admins can do this", HttpStatus.FORBIDDEN);
		}
	}
}
