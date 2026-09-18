package com.yagci.needrelay.web;

import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.OrganizerContactService;
import com.yagci.needrelay.web.dto.OrganizerContactResponse;
import com.yagci.needrelay.web.dto.UpsertOrganizerContactRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
 * Self-service organization contact list for the current organizer.
 */
@RestController
@RequestMapping("/api/me/contacts")
@Tag(name = "contacts")
@SecurityRequirement(name = "bearerAuth")
public class OrganizerContactController {

	private final OrganizerContactService contactService;

	/**
	 * @param contactService contact service
	 */
	public OrganizerContactController(OrganizerContactService contactService) {
		this.contactService = contactService;
	}

	/**
	 * Lists contacts for the current organizer.
	 *
	 * @return contacts
	 */
	@GetMapping
	@Operation(summary = "List organization contacts")
	@ApiResponse(responseCode = "200", description = "Contacts")
	public List<OrganizerContactResponse> list() {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return contactService.list(principal.getId());
	}

	/**
	 * Creates a contact for the current organizer.
	 *
	 * @param request payload
	 * @return created contact
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create organization contact")
	@ApiResponse(responseCode = "201", description = "Created")
	public OrganizerContactResponse create(@Valid @RequestBody UpsertOrganizerContactRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return contactService.create(principal.getId(), request);
	}

	/**
	 * Updates an owned contact.
	 *
	 * @param contactId contact id
	 * @param request payload
	 * @return updated contact
	 */
	@PutMapping("/{contactId}")
	@Operation(summary = "Update organization contact")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OrganizerContactResponse update(
			@PathVariable UUID contactId,
			@Valid @RequestBody UpsertOrganizerContactRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return contactService.update(principal.getId(), contactId, request);
	}

	/**
	 * Deletes an owned contact.
	 *
	 * @param contactId contact id
	 */
	@DeleteMapping("/{contactId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete organization contact")
	@ApiResponse(responseCode = "204", description = "Deleted")
	public void delete(@PathVariable UUID contactId) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		contactService.delete(principal.getId(), contactId);
	}
}
