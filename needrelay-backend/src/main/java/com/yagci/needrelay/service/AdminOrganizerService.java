package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.RefreshTokenRepository;
import com.yagci.needrelay.web.dto.OrganizerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin account management: list, ban, and unban organizers.
 */
@Service
public class AdminOrganizerService {

	private final OrganizerRepository organizerRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	/**
	 * @param organizerRepository organizers
	 * @param refreshTokenRepository session revocation
	 */
	public AdminOrganizerService(
			OrganizerRepository organizerRepository,
			RefreshTokenRepository refreshTokenRepository) {
		this.organizerRepository = organizerRepository;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	/**
	 * Lists all organizers, newest first.
	 *
	 * @return organizers
	 */
	@Transactional(readOnly = true)
	public List<OrganizerResponse> listAll() {
		return organizerRepository.findAll().stream()
				.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
				.map(AuthService::toOrganizerResponse)
				.toList();
	}

	/**
	 * Bans (deactivates) an organizer and revokes sessions.
	 *
	 * @param adminId acting admin
	 * @param organizerId target organizer
	 * @return updated organizer
	 */
	@Transactional
	public OrganizerResponse ban(UUID adminId, UUID organizerId) {
		if (adminId.equals(organizerId)) {
			throw new ApiException("CANNOT_BAN_SELF", "Admins cannot ban themselves", HttpStatus.BAD_REQUEST);
		}
		Organizer target = requireOrganizer(organizerId);
		if (target.getRole() == OrganizerRole.ADMIN) {
			throw new ApiException("CANNOT_BAN_ADMIN", "Cannot ban another admin", HttpStatus.BAD_REQUEST);
		}
		target.setActive(false);
		organizerRepository.save(target);
		refreshTokenRepository.revokeAllActiveForOrganizer(organizerId, Instant.now());
		return AuthService.toOrganizerResponse(target);
	}

	/**
	 * Unbans (reactivates) an organizer.
	 *
	 * @param organizerId target organizer
	 * @return updated organizer
	 */
	@Transactional
	public OrganizerResponse unban(UUID organizerId) {
		Organizer target = requireOrganizer(organizerId);
		target.setActive(true);
		organizerRepository.save(target);
		return AuthService.toOrganizerResponse(target);
	}

	/**
	 * Loads an organizer or throws.
	 *
	 * @param organizerId id
	 * @return organizer
	 */
	private Organizer requireOrganizer(UUID organizerId) {
		return organizerRepository.findById(organizerId)
				.orElseThrow(() -> new ApiException("ORGANIZER_NOT_FOUND", "Organizer not found", HttpStatus.NOT_FOUND));
	}
}
