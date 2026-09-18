package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for password reset tokens.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

	/**
	 * Finds a reset token by hash.
	 *
	 * @param tokenHash SHA-256 hex
	 * @return token if present
	 */
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	/**
	 * Deletes unused tokens for an organizer (invalidate previous resets).
	 *
	 * @param organizerId organizer id
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from PasswordResetToken t where t.organizer.id = :organizerId and t.usedAt is null")
	void deleteUnusedByOrganizerId(@Param("organizerId") UUID organizerId);
}
