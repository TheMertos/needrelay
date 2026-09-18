package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerContact;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizerContactRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.web.dto.OrganizerContactResponse;
import com.yagci.needrelay.web.dto.UpsertOrganizerContactRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for organizer organization contacts.
 */
@Service
public class OrganizerContactService {

	private static final int MAX_CONTACTS = 20;

	private final OrganizerContactRepository contactRepository;
	private final OrganizerRepository organizerRepository;

	/**
	 * @param contactRepository contact persistence
	 * @param organizerRepository organizer persistence
	 */
	public OrganizerContactService(
			OrganizerContactRepository contactRepository,
			OrganizerRepository organizerRepository) {
		this.contactRepository = contactRepository;
		this.organizerRepository = organizerRepository;
	}

	/**
	 * Lists contacts for an organizer.
	 *
	 * @param organizerId owner id
	 * @return contacts
	 */
	@Transactional(readOnly = true)
	public List<OrganizerContactResponse> list(UUID organizerId) {
		return contactRepository.findByOrganizerIdOrderBySortOrderAscCreatedAtAsc(organizerId).stream()
				.map(OrganizerContactService::toResponse)
				.toList();
	}

	/**
	 * Creates a contact for the organizer.
	 *
	 * @param organizerId owner id
	 * @param request payload
	 * @return created contact
	 */
	@Transactional
	public OrganizerContactResponse create(UUID organizerId, UpsertOrganizerContactRequest request) {
		if (contactRepository.countByOrganizerId(organizerId) >= MAX_CONTACTS) {
			throw new ApiException("CONTACT_LIMIT", "Maximum of " + MAX_CONTACTS + " contacts allowed", HttpStatus.BAD_REQUEST);
		}
		Organizer organizer = organizerRepository.findById(organizerId)
				.orElseThrow(() -> new ApiException("ORGANIZER_NOT_FOUND", "Organizer not found", HttpStatus.NOT_FOUND));
		OrganizerContact contact = new OrganizerContact();
		contact.setOrganizer(organizer);
		apply(contact, request);
		contact.setSortOrder((int) contactRepository.countByOrganizerId(organizerId));
		return toResponse(contactRepository.save(contact));
	}

	/**
	 * Updates an owned contact.
	 *
	 * @param organizerId owner id
	 * @param contactId contact id
	 * @param request payload
	 * @return updated contact
	 */
	@Transactional
	public OrganizerContactResponse update(
			UUID organizerId,
			UUID contactId,
			UpsertOrganizerContactRequest request) {
		OrganizerContact contact = requireOwned(organizerId, contactId);
		apply(contact, request);
		return toResponse(contactRepository.save(contact));
	}

	/**
	 * Deletes an owned contact.
	 *
	 * @param organizerId owner id
	 * @param contactId contact id
	 */
	@Transactional
	public void delete(UUID organizerId, UUID contactId) {
		OrganizerContact contact = requireOwned(organizerId, contactId);
		contactRepository.delete(contact);
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
	 * Requires a contact owned by the organizer.
	 *
	 * @param organizerId owner id
	 * @param contactId contact id
	 * @return contact
	 */
	private OrganizerContact requireOwned(UUID organizerId, UUID contactId) {
		return contactRepository.findByIdAndOrganizerId(contactId, organizerId)
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
