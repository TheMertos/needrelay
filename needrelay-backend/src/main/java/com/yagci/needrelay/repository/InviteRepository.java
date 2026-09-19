package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	/**
	 * Finds an invite owned by the given creator.
	 *
	 * @param id invite id
	 * @param createdById organizer id
	 * @return invite if present and owned by that creator
	 */
	Optional<Invite> findByIdAndCreatedById(UUID id, UUID createdById);

	/**
	 * Finds invites with an email address queued for delivery: not yet sent and under
	 * the attempt limit, oldest first so the dispatcher processes them in order.
	 *
	 * @param maxAttempts attempt ceiling; invites at or above this are left for manual retry
	 * @return pending invites
	 */
	@Query("select i from Invite i where i.email is not null and i.emailSentAt is null "
			+ "and i.emailAttempts < :maxAttempts order by i.createdAt asc")
	List<Invite> findPendingEmail(@Param("maxAttempts") int maxAttempts);
}
