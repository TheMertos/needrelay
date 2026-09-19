package com.yagci.needrelay.service;

import com.yagci.needrelay.common.UuidV7;
import com.yagci.needrelay.domain.Invite;
import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.InviteRepository;
import com.yagci.needrelay.repository.OrganizationRepository;
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
 * Invite creation and listing. Invite emails are queued here and delivered asynchronously
 * by {@link InviteEmailDispatchService}.
 */
@Service
public class InviteService {

	private final InviteRepository inviteRepository;
	private final OrganizerRepository organizerRepository;
	private final OrganizationRepository organizationRepository;

	/**
	 * @param inviteRepository invite persistence
	 * @param organizerRepository organizer lookup
	 * @param organizationRepository organization lookup
	 */
	public InviteService(
			InviteRepository inviteRepository,
			OrganizerRepository organizerRepository,
			OrganizationRepository organizationRepository) {
		this.inviteRepository = inviteRepository;
		this.organizerRepository = organizerRepository;
		this.organizationRepository = organizationRepository;
	}

	/**
	 * Creates an invite and, when an address is given, queues it for async email delivery.
	 * A platform admin must specify a target organization (and may choose the granted role);
	 * an organization admin can only invite into their own organization, always as {@code USER}.
	 *
	 * @param creatorId creating organizer id
	 * @param request optional email/daysValid, plus target org/role
	 * @return created invite
	 */
	@Transactional
	public InviteResponse createInvite(UUID creatorId, CreateInviteRequest request) {
		Organizer creator = organizerRepository.findById(creatorId)
				.orElseThrow(() -> new ApiException("ORGANIZER_NOT_FOUND", "Organizer not found", HttpStatus.NOT_FOUND));

		Organization targetOrganization;
		OrganizationRole targetRole;
		if (creator.getRole() == OrganizerRole.ADMIN) {
			if (request.organizationId() == null) {
				throw new ApiException(
						"ORGANIZATION_REQUIRED", "organizationId is required for admin-created invites", HttpStatus.BAD_REQUEST);
			}
			targetOrganization = organizationRepository.findById(request.organizationId())
					.orElseThrow(() -> new ApiException("ORGANIZATION_NOT_FOUND", "Organization not found", HttpStatus.NOT_FOUND));
			targetRole = request.organizationRole() != null ? request.organizationRole() : OrganizationRole.USER;
		} else {
			if (creator.getOrganizationRole() != OrganizationRole.ADMIN) {
				throw new ApiException(
						"FORBIDDEN", "Only organization admins can invite new members", HttpStatus.FORBIDDEN);
			}
			targetOrganization = creator.getOrganization();
			targetRole = OrganizationRole.USER;
		}

		int daysValid = request.daysValid() != null ? request.daysValid() : 7;
		Invite invite = new Invite();
		invite.setToken(UuidV7.generate().toString().replace("-", "")
				+ UuidV7.generate().toString().replace("-", "").substring(0, 32));
		if (request.email() != null && !request.email().isBlank()) {
			invite.setEmail(request.email().trim().toLowerCase());
		}
		invite.setCreatedBy(creator);
		invite.setOrganization(targetOrganization);
		invite.setOrganizationRole(targetRole);
		invite.setExpiresAt(Instant.now().plus(daysValid, ChronoUnit.DAYS));
		inviteRepository.save(invite);
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
	 * Revokes (deletes) an unused invite created by the given organizer.
	 *
	 * @param creatorId creating organizer id
	 * @param inviteId invite id
	 */
	@Transactional
	public void revoke(UUID creatorId, UUID inviteId) {
		Invite invite = inviteRepository.findByIdAndCreatedById(inviteId, creatorId)
				.orElseThrow(() -> new ApiException("INVITE_NOT_FOUND", "Invite not found", HttpStatus.NOT_FOUND));
		if (invite.getUsedAt() != null) {
			throw new ApiException(
					"INVITE_ALREADY_USED", "Used invites cannot be revoked", HttpStatus.CONFLICT);
		}
		inviteRepository.delete(invite);
	}

	/**
	 * Maps an invite entity to the response DTO.
	 *
	 * @param invite entity
	 * @return response DTO
	 */
	private InviteResponse toResponse(Invite invite) {
		Organization organization = invite.getOrganization();
		return new InviteResponse(
				invite.getId(),
				invite.getToken(),
				invite.getEmail(),
				organization != null ? organization.getId() : null,
				organization != null ? organization.getName() : null,
				invite.getOrganizationRole(),
				invite.getExpiresAt(),
				invite.getUsedAt(),
				invite.getCreatedAt(),
				invite.getEmailSentAt(),
				invite.getEmailError());
	}
}
