package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
