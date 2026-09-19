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
	 * Lists organization-wide contacts (not tied to a specific relief request) for an
	 * organization, ordered by sort order then creation.
	 *
	 * @param organizationId organization id
	 * @return contacts
	 */
	List<OrganizerContact> findByOrganizationIdAndReliefRequestIsNullOrderBySortOrderAscCreatedAtAsc(UUID organizationId);

	/**
	 * Counts organization-wide contacts owned by an organization.
	 *
	 * @param organizationId organization id
	 * @return count
	 */
	long countByOrganizationIdAndReliefRequestIsNull(UUID organizationId);

	/**
	 * Finds an organization-wide contact owned by an organization.
	 *
	 * @param id contact id
	 * @param organizationId organization id
	 * @return contact if present
	 */
	Optional<OrganizerContact> findByIdAndOrganizationIdAndReliefRequestIsNull(UUID id, UUID organizationId);

	/**
	 * Lists contacts specific to a relief request, ordered by sort order then creation.
	 *
	 * @param reliefRequestId relief request id
	 * @return contacts
	 */
	List<OrganizerContact> findByReliefRequestIdOrderBySortOrderAscCreatedAtAsc(UUID reliefRequestId);

	/**
	 * Counts contacts specific to a relief request.
	 *
	 * @param reliefRequestId relief request id
	 * @return count
	 */
	long countByReliefRequestId(UUID reliefRequestId);

	/**
	 * Finds a contact specific to a relief request.
	 *
	 * @param id contact id
	 * @param reliefRequestId relief request id
	 * @return contact if present
	 */
	Optional<OrganizerContact> findByIdAndReliefRequestId(UUID id, UUID reliefRequestId);
}
