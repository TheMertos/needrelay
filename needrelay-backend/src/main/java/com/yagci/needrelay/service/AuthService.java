package com.yagci.needrelay.service;

import com.yagci.needrelay.common.UuidV7;
import com.yagci.needrelay.config.NeedRelayProperties;
import com.yagci.needrelay.domain.Invite;
import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.PasswordResetToken;
import com.yagci.needrelay.domain.RefreshToken;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.InviteRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.PasswordResetTokenRepository;
import com.yagci.needrelay.repository.RefreshTokenRepository;
import com.yagci.needrelay.security.JwtService;
import com.yagci.needrelay.web.dto.ChangePasswordRequest;
import com.yagci.needrelay.web.dto.ForgotPasswordRequest;
import com.yagci.needrelay.web.dto.LoginRequest;
import com.yagci.needrelay.web.dto.LogoutRequest;
import com.yagci.needrelay.web.dto.OrganizerResponse;
import com.yagci.needrelay.web.dto.RefreshRequest;
import com.yagci.needrelay.web.dto.RegisterRequest;
import com.yagci.needrelay.web.dto.ResetPasswordRequest;
import com.yagci.needrelay.web.dto.TokenResponse;
import com.yagci.needrelay.web.dto.UpdateProfileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Registration, login, password flows, profile, and token lifecycle.
 */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final OrganizerRepository organizerRepository;
	private final InviteRepository inviteRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final NeedRelayProperties properties;
	private final EmailService emailService;

	/**
	 * @param organizerRepository organizers
	 * @param inviteRepository invites
	 * @param refreshTokenRepository refresh tokens
	 * @param passwordResetTokenRepository password reset tokens
	 * @param passwordEncoder BCrypt encoder
	 * @param jwtService access JWT issuer
	 * @param properties JWT and app settings
	 * @param emailService Resend email sender
	 */
	public AuthService(
			OrganizerRepository organizerRepository,
			InviteRepository inviteRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordResetTokenRepository passwordResetTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			NeedRelayProperties properties,
			EmailService emailService) {
		this.organizerRepository = organizerRepository;
		this.inviteRepository = inviteRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.properties = properties;
		this.emailService = emailService;
	}

	/**
	 * Registers a new ORGANIZER using a valid invite token.
	 *
	 * @param request registration payload
	 * @return access/refresh token pair
	 */
	@Transactional
	public TokenResponse register(RegisterRequest request) {
		Invite invite = inviteRepository.findByToken(request.inviteToken())
				.orElseThrow(() -> new ApiException("INVITE_INVALID", "Invite not found", HttpStatus.BAD_REQUEST));
		if (!invite.isUsable()) {
			throw new ApiException("INVITE_INVALID", "Invite is expired or already used", HttpStatus.BAD_REQUEST);
		}
		String email = request.email().trim().toLowerCase();
		if (invite.getEmail() != null && !invite.getEmail().equalsIgnoreCase(email)) {
			throw new ApiException("INVITE_EMAIL_MISMATCH", "Email does not match invite", HttpStatus.BAD_REQUEST);
		}
		if (organizerRepository.findByEmailIgnoreCase(email).isPresent()) {
			throw new ApiException("EMAIL_TAKEN", "Email already registered", HttpStatus.CONFLICT);
		}
		Organization organization = invite.getOrganization();
		if (organization == null) {
			throw new ApiException("INVITE_INVALID", "Invite has no target organization", HttpStatus.BAD_REQUEST);
		}

		Organizer organizer = new Organizer();
		organizer.setEmail(email);
		organizer.setPasswordHash(passwordEncoder.encode(request.password()));
		organizer.setDisplayName(request.displayName().trim());
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizer.setOrganization(organization);
		organizer.setOrganizationRole(invite.getOrganizationRole());
		organizer.setActive(true);
		organizerRepository.save(organizer);

		invite.setUsedAt(Instant.now());
		inviteRepository.save(invite);

		return issueTokens(organizer);
	}

	/**
	 * Authenticates an organizer with email and password.
	 *
	 * @param request login payload
	 * @return access/refresh token pair
	 */
	@Transactional
	public TokenResponse login(LoginRequest request) {
		Organizer organizer = organizerRepository.findByEmailIgnoreCase(request.email().trim())
				.orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED));
		if (!organizer.isActive() || !passwordEncoder.matches(request.password(), organizer.getPasswordHash())) {
			throw new ApiException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED);
		}
		if (organizer.getOrganization() != null && !organizer.getOrganization().isActive()) {
			throw new ApiException("ORGANIZATION_DISABLED", "Organization is disabled", HttpStatus.UNAUTHORIZED);
		}
		return issueTokens(organizer);
	}

	/**
	 * Rotates a refresh token and issues a new access token.
	 *
	 * @param request refresh payload
	 * @return new token pair
	 */
	@Transactional
	public TokenResponse refresh(RefreshRequest request) {
		RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256Hex(request.refreshToken()))
				.orElseThrow(() -> new ApiException("REFRESH_INVALID", "Refresh token invalid", HttpStatus.UNAUTHORIZED));
		if (!stored.isActive()) {
			throw new ApiException("REFRESH_INVALID", "Refresh token expired or revoked", HttpStatus.UNAUTHORIZED);
		}
		Organizer organizer = stored.getOrganizer();
		if (!organizer.isActive()) {
			throw new ApiException("ACCOUNT_DISABLED", "Account is disabled", HttpStatus.UNAUTHORIZED);
		}
		if (organizer.getOrganization() != null && !organizer.getOrganization().isActive()) {
			throw new ApiException("ORGANIZATION_DISABLED", "Organization is disabled", HttpStatus.UNAUTHORIZED);
		}
		stored.setRevokedAt(Instant.now());
		refreshTokenRepository.save(stored);
		return issueTokens(organizer);
	}

	/**
	 * Revokes the given refresh token if present.
	 *
	 * @param request logout payload
	 */
	@Transactional
	public void logout(LogoutRequest request) {
		refreshTokenRepository.findByTokenHash(sha256Hex(request.refreshToken())).ifPresent(token -> {
			if (token.getRevokedAt() == null) {
				token.setRevokedAt(Instant.now());
				refreshTokenRepository.save(token);
			}
		});
	}

	/**
	 * Returns the current organizer profile.
	 *
	 * @param organizerId current user id
	 * @return profile
	 */
	@Transactional(readOnly = true)
	public OrganizerResponse me(UUID organizerId) {
		return toOrganizerResponse(requireOrganizer(organizerId));
	}

	/**
	 * Updates display name for the current organizer.
	 *
	 * @param organizerId current user id
	 * @param request profile payload
	 * @return updated profile
	 */
	@Transactional
	public OrganizerResponse updateProfile(UUID organizerId, UpdateProfileRequest request) {
		Organizer organizer = requireOrganizer(organizerId);
		organizer.setDisplayName(request.displayName().trim());
		organizerRepository.save(organizer);
		return toOrganizerResponse(organizer);
	}

	/**
	 * Changes password for the authenticated organizer.
	 *
	 * @param organizerId current user id
	 * @param request current and new password
	 */
	@Transactional
	public void changePassword(UUID organizerId, ChangePasswordRequest request) {
		Organizer organizer = requireOrganizer(organizerId);
		if (!passwordEncoder.matches(request.currentPassword(), organizer.getPasswordHash())) {
			throw new ApiException("INVALID_PASSWORD", "Current password is incorrect", HttpStatus.BAD_REQUEST);
		}
		organizer.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		organizerRepository.save(organizer);
		refreshTokenRepository.revokeAllActiveForOrganizer(organizerId, Instant.now());
	}

	/**
	 * Starts a password reset; always succeeds to avoid email enumeration.
	 *
	 * @param request email payload
	 */
	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		String email = request.email().trim().toLowerCase();
		organizerRepository.findByEmailIgnoreCase(email).ifPresent(organizer -> {
			if (!organizer.isActive()) {
				return;
			}
			passwordResetTokenRepository.deleteUnusedByOrganizerId(organizer.getId());
			String rawToken = UuidV7.generate().toString().replace("-", "")
					+ UuidV7.generate().toString().replace("-", "");
			PasswordResetToken token = new PasswordResetToken();
			token.setOrganizer(organizer);
			token.setTokenHash(sha256Hex(rawToken));
			token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
			passwordResetTokenRepository.save(token);
			// Best-effort: this method always succeeds to avoid email enumeration, so a
			// transient (or unconfigured) email provider must not surface as an error here.
			try {
				emailService.sendPasswordReset(organizer.getEmail(), rawToken);
			} catch (Exception ex) {
				log.error("Failed to email password reset to {}: {}", organizer.getEmail(), ex.getMessage());
			}
		});
	}

	/**
	 * Completes password reset using the emailed token.
	 *
	 * @param request token and new password
	 */
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(sha256Hex(request.token()))
				.orElseThrow(() -> new ApiException("RESET_INVALID", "Reset token invalid", HttpStatus.BAD_REQUEST));
		if (!token.isUsable()) {
			throw new ApiException("RESET_INVALID", "Reset token expired or already used", HttpStatus.BAD_REQUEST);
		}
		Organizer organizer = token.getOrganizer();
		if (!organizer.isActive()) {
			throw new ApiException("ACCOUNT_DISABLED", "Account is disabled", HttpStatus.FORBIDDEN);
		}
		organizer.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		organizerRepository.save(organizer);
		token.setUsedAt(Instant.now());
		passwordResetTokenRepository.save(token);
		refreshTokenRepository.revokeAllActiveForOrganizer(organizer.getId(), Instant.now());
	}

	/**
	 * Maps an organizer entity to the response DTO.
	 *
	 * @param organizer entity
	 * @return response DTO
	 */
	public static OrganizerResponse toOrganizerResponse(Organizer organizer) {
		Organization organization = organizer.getOrganization();
		return new OrganizerResponse(
				organizer.getId(),
				organizer.getEmail(),
				organizer.getDisplayName(),
				organizer.getRole(),
				organizer.isActive(),
				organization != null ? organization.getId() : null,
				organization != null ? organization.getName() : null,
				organizer.getOrganizationRole(),
				organizer.getCreatedAt());
	}

	/**
	 * Issues access + refresh tokens and persists the refresh hash.
	 *
	 * @param organizer authenticated organizer
	 * @return token response
	 */
	private TokenResponse issueTokens(Organizer organizer) {
		Organization organization = organizer.getOrganization();
		String accessToken = jwtService.createAccessToken(
				organizer.getId(),
				organizer.getEmail(),
				organizer.getRole(),
				organization != null ? organization.getId() : null,
				organizer.getOrganizationRole());
		String rawRefresh = UuidV7.generate() + "." + UuidV7.generate();
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setOrganizer(organizer);
		refreshToken.setTokenHash(sha256Hex(rawRefresh));
		refreshToken.setExpiresAt(Instant.now().plus(properties.getJwt().getRefreshTokenDays(), ChronoUnit.DAYS));
		refreshTokenRepository.save(refreshToken);
		return new TokenResponse(
				accessToken,
				rawRefresh,
				jwtService.getAccessTokenMinutes(),
				toOrganizerResponse(organizer));
	}

	/**
	 * Loads an organizer or throws not found.
	 *
	 * @param organizerId id
	 * @return organizer
	 */
	private Organizer requireOrganizer(UUID organizerId) {
		return organizerRepository.findById(organizerId)
				.orElseThrow(() -> new ApiException("ORGANIZER_NOT_FOUND", "Organizer not found", HttpStatus.NOT_FOUND));
	}

	/**
	 * Hashes a raw token with SHA-256 and returns lowercase hex.
	 *
	 * @param rawToken opaque token
	 * @return SHA-256 hex digest
	 */
	static String sha256Hex(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
