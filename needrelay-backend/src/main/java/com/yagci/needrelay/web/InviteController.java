package com.yagci.needrelay.web;

import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.InviteService;
import com.yagci.needrelay.web.dto.CreateInviteRequest;
import com.yagci.needrelay.web.dto.InviteResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Invite management for authenticated organizers.
 */
@RestController
@RequestMapping("/api/invites")
@Tag(name = "Invites")
@AllArgsConstructor
public class InviteController {

	private final InviteService inviteService;

	/**
	 * Creates a new invite.
	 *
	 * @param request create payload
	 * @return created invite
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create invite")
	@ApiResponse(responseCode = "201", description = "Invite created")
	public InviteResponse create(@Valid @RequestBody CreateInviteRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return inviteService.createInvite(principal.getId(), request);
	}

	/**
	 * Lists invites created by the current organizer.
	 *
	 * @return invites
	 */
	@GetMapping
	@Operation(summary = "List my invites")
	@ApiResponse(responseCode = "200", description = "Invite list")
	public List<InviteResponse> listMine() {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return inviteService.listMine(principal.getId());
	}

	/**
	 * Revokes an unused invite created by the current organizer.
	 *
	 * @param inviteId invite id
	 */
	@DeleteMapping("/{inviteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Revoke invite")
	@ApiResponse(responseCode = "204", description = "Revoked")
	public void revoke(@PathVariable UUID inviteId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		inviteService.revoke(principal.getId(), inviteId);
	}
}
