package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for refresh tokens.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	/**
	 * Finds a refresh token by its hashed value.
	 *
	 * @param tokenHash SHA-256 hex of the raw token
	 * @return token entity if present
	 */
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * Revokes all active refresh tokens for an organizer.
	 *
	 * @param organizerId organizer id
	 * @param revokedAt revocation timestamp
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update RefreshToken t
			set t.revokedAt = :revokedAt
			where t.organizer.id = :organizerId and t.revokedAt is null
			""")
	void revokeAllActiveForOrganizer(@Param("organizerId") UUID organizerId, @Param("revokedAt") Instant revokedAt);
}
