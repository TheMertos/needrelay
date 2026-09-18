package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.OrganizerContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for organizer contacts.
 */
public interface OrganizerContactRepository extends JpaRepository<OrganizerContact, UUID> {

	/**
	 * Lists contacts for an organizer ordered by sort order then creation.
	 *
	 * @param organizerId organizer id
	 * @return contacts
	 */
	List<OrganizerContact> findByOrganizerIdOrderBySortOrderAscCreatedAtAsc(UUID organizerId);

	/**
	 * Counts contacts owned by an organizer.
	 *
	 * @param organizerId organizer id
	 * @return count
	 */
	long countByOrganizerId(UUID organizerId);

	/**
	 * Finds a contact owned by an organizer.
	 *
	 * @param id contact id
	 * @param organizerId organizer id
	 * @return contact if present
	 */
	Optional<OrganizerContact> findByIdAndOrganizerId(UUID id, UUID organizerId);
}
