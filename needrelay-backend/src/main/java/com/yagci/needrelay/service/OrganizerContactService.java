package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizerContact;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerContactRepository;
import com.yagci.needrelay.web.dto.OrganizerContactResponse;
import com.yagci.needrelay.web.dto.UpsertOrganizerContactRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for organizer organization contacts, both organization-wide and relief-request-specific.
 */
@Service
@AllArgsConstructor
public class OrganizerContactService {

	private static final int MAX_CONTACTS = 20;
	private static final int MAX_REQUEST_CONTACTS = 10;

	private final OrganizerContactRepository contactRepository;
	private final OrganizationRepository organizationRepository;
	private final ReliefRequestService reliefRequestService;

	/**
	 * Lists organization-wide contacts for an organization.
	 *
	 * @param organizationId owner id
	 * @return contacts
	 */
	@Transactional(readOnly = true)
	public List<OrganizerContactResponse> list(UUID organizationId) {
		return contactRepository
				.findByOrganizationIdAndReliefRequestIsNullOrderBySortOrderAscCreatedAtAsc(organizationId)
				.stream()
				.map(OrganizerContactService::toResponse)
				.toList();
	}

	/**
	 * Creates an organization-wide contact.
	 *
	 * @param organizationId owner id
	 * @param request payload
	 * @return created contact
	 */
	@Transactional
	public OrganizerContactResponse create(UUID organizationId, UpsertOrganizerContactRequest request) {
		if (contactRepository.countByOrganizationIdAndReliefRequestIsNull(organizationId) >= MAX_CONTACTS) {
			throw new ApiException("CONTACT_LIMIT", "Maximum of " + MAX_CONTACTS + " contacts allowed", HttpStatus.BAD_REQUEST);
		}
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ApiException("ORGANIZATION_NOT_FOUND", "Organization not found", HttpStatus.NOT_FOUND));
		OrganizerContact contact = new OrganizerContact();
		contact.setOrganization(organization);
		apply(contact, request);
		contact.setSortOrder((int) contactRepository.countByOrganizationIdAndReliefRequestIsNull(organizationId));
		return toResponse(contactRepository.save(contact));
	}

	/**
	 * Updates an owned organization-wide contact.
	 *
	 * @param organizationId owner id
	 * @param contactId contact id
	 * @param request payload
	 * @return updated contact
	 */
	@Transactional
	public OrganizerContactResponse update(
			UUID organizationId,
			UUID contactId,
			UpsertOrganizerContactRequest request) {
		OrganizerContact contact = requireOwned(organizationId, contactId);
		apply(contact, request);
		return toResponse(contactRepository.save(contact));
	}

	/**
	 * Deletes an owned organization-wide contact.
	 *
	 * @param organizationId owner id
	 * @param contactId contact id
	 */
	@Transactional
	public void delete(UUID organizationId, UUID contactId) {
		OrganizerContact contact = requireOwned(organizationId, contactId);
		contactRepository.delete(contact);
	}

	/**
	 * Lists contacts specific to an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @return contacts
	 */
	@Transactional(readOnly = true)
	public List<OrganizerContactResponse> listForRequest(UUID organizerId, UUID requestId) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		return contactRepository.findByReliefRequestIdOrderBySortOrderAscCreatedAtAsc(requestId).stream()
				.map(OrganizerContactService::toResponse)
				.toList();
	}

	/**
	 * Creates a contact specific to an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @param request payload
	 * @return created contact
	 */
	@Transactional
	public OrganizerContactResponse createForRequest(
			UUID organizerId,
			UUID requestId,
			UpsertOrganizerContactRequest request) {
		ReliefRequest reliefRequest = reliefRequestService.getOwnedEntity(organizerId, requestId);
		if (contactRepository.countByReliefRequestId(requestId) >= MAX_REQUEST_CONTACTS) {
			throw new ApiException(
					"CONTACT_LIMIT", "Maximum of " + MAX_REQUEST_CONTACTS + " contacts allowed", HttpStatus.BAD_REQUEST);
		}
		OrganizerContact contact = new OrganizerContact();
		contact.setOrganization(reliefRequest.getOrganization());
		contact.setReliefRequest(reliefRequest);
		apply(contact, request);
		contact.setSortOrder((int) contactRepository.countByReliefRequestId(requestId));
		return toResponse(contactRepository.save(contact));
	}

	/**
	 * Updates a contact specific to an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @param contactId contact id
	 * @param request payload
	 * @return updated contact
	 */
	@Transactional
	public OrganizerContactResponse updateForRequest(
			UUID organizerId,
			UUID requestId,
			UUID contactId,
			UpsertOrganizerContactRequest request) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		OrganizerContact contact = requireOwnedByRequest(requestId, contactId);
		apply(contact, request);
		return toResponse(contactRepository.save(contact));
	}

	/**
	 * Deletes a contact specific to an owned relief request.
	 *
	 * @param organizerId owner id
	 * @param requestId relief request id
	 * @param contactId contact id
	 */
	@Transactional
	public void deleteForRequest(UUID organizerId, UUID requestId, UUID contactId) {
		reliefRequestService.getOwnedEntity(organizerId, requestId);
		OrganizerContact contact = requireOwnedByRequest(requestId, contactId);
		contactRepository.delete(contact);
	}

	/**
	 * Lists contacts specific to a relief request for public display (no ownership check).
	 *
	 * @param requestId relief request id
	 * @return contacts
	 */
	@Transactional(readOnly = true)
	public List<OrganizerContactResponse> listForRequestPublic(UUID requestId) {
		return contactRepository.findByReliefRequestIdOrderBySortOrderAscCreatedAtAsc(requestId).stream()
				.map(OrganizerContactService::toResponse)
				.toList();
	}

	/**
	 * Applies upsert fields onto a contact entity.
	 *
	 * @param contact entity
	 * @param request payload
	 */
	private static void apply(OrganizerContact contact, UpsertOrganizerContactRequest request) {
		contact.setName(request.name().trim());
		contact.setRole(request.role().trim());
		contact.setPhone(request.phone().trim());
		contact.setEmail(request.email().trim().toLowerCase());
		contact.setNote(blankToNull(request.note()));
	}

	/**
	 * Requires an organization-wide contact owned by the organization.
	 *
	 * @param organizationId owner id
	 * @param contactId contact id
	 * @return contact
	 */
	private OrganizerContact requireOwned(UUID organizationId, UUID contactId) {
		return contactRepository.findByIdAndOrganizationIdAndReliefRequestIsNull(contactId, organizationId)
				.orElseThrow(() -> new ApiException("CONTACT_NOT_FOUND", "Contact not found", HttpStatus.NOT_FOUND));
	}

	/**
	 * Requires a contact scoped to the given relief request.
	 *
	 * @param requestId relief request id
	 * @param contactId contact id
	 * @return contact
	 */
	private OrganizerContact requireOwnedByRequest(UUID requestId, UUID contactId) {
		return contactRepository.findByIdAndReliefRequestId(contactId, requestId)
				.orElseThrow(() -> new ApiException("CONTACT_NOT_FOUND", "Contact not found", HttpStatus.NOT_FOUND));
	}

	/**
	 * Maps entity to response DTO.
	 *
	 * @param contact entity
	 * @return response
	 */
	public static OrganizerContactResponse toResponse(OrganizerContact contact) {
		return new OrganizerContactResponse(
				contact.getId(),
				contact.getName(),
				contact.getRole(),
				contact.getPhone(),
				contact.getEmail(),
				contact.getNote(),
				contact.getSortOrder());
	}

	/**
	 * Trims blank notes to null.
	 *
	 * @param value raw note
	 * @return trimmed note or null
	 */
	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
