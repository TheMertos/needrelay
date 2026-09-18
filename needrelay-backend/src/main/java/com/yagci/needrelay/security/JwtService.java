package com.yagci.needrelay.security;

import com.yagci.needrelay.config.NeedRelayProperties;
import com.yagci.needrelay.domain.OrganizerRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Creates and parses HS256 access JWTs for organizers.
 */
@Component
public class JwtService {

	private final SecretKey key;
	private final long accessTokenMinutes;

	/**
	 * Builds the signing key from configured secret.
	 *
	 * @param properties NeedRelay properties
	 */
	public JwtService(NeedRelayProperties properties) {
		byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
		this.key = Keys.hmacShaKeyFor(secretBytes);
		this.accessTokenMinutes = properties.getJwt().getAccessTokenMinutes();
	}

	/**
	 * Issues a short-lived access token.
	 *
	 * @param organizerId organizer id
	 * @param email email
	 * @param role role
	 * @return JWT compact string
	 */
	public String createAccessToken(UUID organizerId, String email, OrganizerRole role) {
		Instant now = Instant.now();
		Instant expiry = now.plusSeconds(accessTokenMinutes * 60);
		return Jwts.builder()
				.subject(organizerId.toString())
				.claim("email", email)
				.claim("role", role.name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(key)
				.compact();
	}

	/**
	 * Parses and validates an access token.
	 *
	 * @param token JWT compact string
	 * @return claims
	 */
	public Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * Returns configured access token lifetime in minutes.
	 *
	 * @return minutes
	 */
	public long getAccessTokenMinutes() {
		return accessTokenMinutes;
	}
}
