package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for organizers.
 */
public interface OrganizerRepository extends JpaRepository<Organizer, UUID> {

	/**
	 * Finds an organizer by email (case-sensitive storage, normalized by service).
	 *
	 * @param email email address
	 * @return organizer if present
	 */
	Optional<Organizer> findByEmailIgnoreCase(String email);

	/**
	 * Lists members of an organization, oldest first.
	 *
	 * @param organizationId organization id
	 * @return members
	 */
	List<Organizer> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

	/**
	 * Counts members of an organization with the given org role.
	 *
	 * @param organizationId organization id
	 * @param organizationRole role to count
	 * @return count
	 */
	long countByOrganizationIdAndOrganizationRole(UUID organizationId, OrganizationRole organizationRole);

	/**
	 * Finds a member of the given organization by id.
	 *
	 * @param id organizer id
	 * @param organizationId organization id
	 * @return organizer if present and a member of that organization
	 */
	Optional<Organizer> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
