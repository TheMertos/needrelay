package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for invites.
 */
public interface InviteRepository extends JpaRepository<Invite, UUID> {

	/**
	 * Finds an invite by its public token.
	 *
	 * @param token invite token
	 * @return invite if present
	 */
	Optional<Invite> findByToken(String token);

	/**
	 * Lists invites created by an organizer, newest first.
	 *
	 * @param createdById organizer id
	 * @return invites
	 */
	List<Invite> findByCreatedByIdOrderByCreatedAtDesc(UUID createdById);
}
