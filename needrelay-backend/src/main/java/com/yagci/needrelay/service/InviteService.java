package com.yagci.needrelay.service;

import com.yagci.needrelay.common.UuidV7;
import com.yagci.needrelay.domain.Invite;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.InviteRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.web.dto.CreateInviteRequest;
import com.yagci.needrelay.web.dto.InviteResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Invite creation, listing, and email delivery.
 */
@Service
public class InviteService {

	private final InviteRepository inviteRepository;
	private final OrganizerRepository organizerRepository;
	private final EmailService emailService;

	/**
	 * @param inviteRepository invite persistence
	 * @param organizerRepository organizer lookup
	 * @param emailService invite email sender
	 */
	public InviteService(
			InviteRepository inviteRepository,
			OrganizerRepository organizerRepository,
			EmailService emailService) {
		this.inviteRepository = inviteRepository;
		this.organizerRepository = organizerRepository;
		this.emailService = emailService;
	}

	/**
	 * Creates an invite and emails the recipient when an address is provided.
	 *
	 * @param creatorId creating organizer id
	 * @param request optional email and daysValid
	 * @return created invite
	 */
	@Transactional
	public InviteResponse createInvite(UUID creatorId, CreateInviteRequest request) {
		Organizer creator = organizerRepository.findById(creatorId)
				.orElseThrow(() -> new ApiException("ORGANIZER_NOT_FOUND", "Organizer not found", HttpStatus.NOT_FOUND));
		int daysValid = request.daysValid() != null ? request.daysValid() : 7;
		Invite invite = new Invite();
		invite.setToken(UuidV7.generate().toString().replace("-", "")
				+ UuidV7.generate().toString().replace("-", "").substring(0, 32));
		String email = null;
		if (request.email() != null && !request.email().isBlank()) {
			email = request.email().trim().toLowerCase();
			invite.setEmail(email);
		}
		invite.setCreatedBy(creator);
		invite.setExpiresAt(Instant.now().plus(daysValid, ChronoUnit.DAYS));
		inviteRepository.save(invite);
		if (email != null) {
			emailService.sendInvite(email, invite.getToken());
		}
		return toResponse(invite);
	}

	/**
	 * Lists invites created by the given organizer, newest first.
	 *
	 * @param creatorId organizer id
	 * @return invites
	 */
	@Transactional(readOnly = true)
	public List<InviteResponse> listMine(UUID creatorId) {
		return inviteRepository.findByCreatedByIdOrderByCreatedAtDesc(creatorId).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Maps an invite entity to the response DTO.
	 *
	 * @param invite entity
	 * @return response DTO
	 */
	private InviteResponse toResponse(Invite invite) {
		return new InviteResponse(
				invite.getId(),
				invite.getToken(),
				invite.getEmail(),
				invite.getExpiresAt(),
				invite.getUsedAt(),
				invite.getCreatedAt());
	}
}
