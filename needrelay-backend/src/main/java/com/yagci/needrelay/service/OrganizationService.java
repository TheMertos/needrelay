package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.RefreshTokenRepository;
import com.yagci.needrelay.web.dto.OrganizationMemberResponse;
import com.yagci.needrelay.web.dto.OrganizationResponse;
import com.yagci.needrelay.web.dto.UpdateMemberRoleRequest;
import com.yagci.needrelay.web.dto.UpdateOrganizationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Organization profile and member management for the current organization.
 */
@Service
public class OrganizationService {

	private final OrganizationRepository organizationRepository;
	private final OrganizerRepository organizerRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	/**
	 * @param organizationRepository organizations
	 * @param organizerRepository member accounts
	 * @param refreshTokenRepository session revocation on member removal
	 */
	public OrganizationService(
			OrganizationRepository organizationRepository,
			OrganizerRepository organizerRepository,
			RefreshTokenRepository refreshTokenRepository) {
		this.organizationRepository = organizationRepository;
		this.organizerRepository = organizerRepository;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	/**
	 * Returns the current organization's profile.
	 *
	 * @param organizationId organization id
	 * @return profile
	 */
	@Transactional(readOnly = true)
	public OrganizationResponse get(UUID organizationId) {
		return toResponse(requireOrganization(organizationId));
	}

	/**
	 * Updates the organization's name/description. Organization admin only (enforced by caller).
	 *
	 * @param organizationId organization id
	 * @param request payload
	 * @return updated profile
	 */
	@Transactional
	public OrganizationResponse update(UUID organizationId, UpdateOrganizationRequest request) {
		Organization organization = requireOrganization(organizationId);
		organization.setName(request.name().trim());
		organization.setDescription(blankToNull(request.description()));
		organizationRepository.save(organization);
		return toResponse(organization);
	}

	/**
	 * Lists members of the organization, oldest first.
	 *
	 * @param organizationId organization id
	 * @return members
	 */
	@Transactional(readOnly = true)
	public List<OrganizationMemberResponse> listMembers(UUID organizationId) {
		return organizerRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId).stream()
				.map(OrganizationService::toMemberResponse)
				.toList();
	}

	/**
	 * Changes a member's role. Rejects demoting the organization's last remaining admin.
	 *
	 * @param organizationId organization id
	 * @param memberId member to update
	 * @param request new role
	 * @return updated member
	 */
	@Transactional
	public OrganizationMemberResponse updateMemberRole(
			UUID organizationId,
			UUID memberId,
			UpdateMemberRoleRequest request) {
		Organizer member = requireMember(organizationId, memberId);
		if (member.getOrganizationRole() == OrganizationRole.ADMIN
				&& request.organizationRole() == OrganizationRole.USER
				&& organizerRepository.countByOrganizationIdAndOrganizationRole(organizationId, OrganizationRole.ADMIN) <= 1) {
			throw new ApiException(
					"LAST_ADMIN", "Cannot demote the organization's last remaining admin", HttpStatus.CONFLICT);
		}
		member.setOrganizationRole(request.organizationRole());
		organizerRepository.save(member);
		return toMemberResponse(member);
	}

	/**
	 * Deactivates a member and revokes their sessions. Rejects removing the organization's
	 * last remaining admin or acting on oneself.
	 *
	 * @param organizationId organization id
	 * @param actingMemberId the acting admin's own id
	 * @param memberId member to remove
	 */
	@Transactional
	public void removeMember(UUID organizationId, UUID actingMemberId, UUID memberId) {
		if (actingMemberId.equals(memberId)) {
			throw new ApiException("CANNOT_REMOVE_SELF", "You cannot remove yourself", HttpStatus.BAD_REQUEST);
		}
		Organizer member = requireMember(organizationId, memberId);
		if (member.getOrganizationRole() == OrganizationRole.ADMIN
				&& organizerRepository.countByOrganizationIdAndOrganizationRole(organizationId, OrganizationRole.ADMIN) <= 1) {
			throw new ApiException(
					"LAST_ADMIN", "Cannot remove the organization's last remaining admin", HttpStatus.CONFLICT);
		}
		member.setActive(false);
		organizerRepository.save(member);
		refreshTokenRepository.revokeAllActiveForOrganizer(memberId, Instant.now());
	}

	/**
	 * Loads an organization or throws.
	 *
	 * @param organizationId id
	 * @return organization
	 */
	private Organization requireOrganization(UUID organizationId) {
		return organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ApiException("ORGANIZATION_NOT_FOUND", "Organization not found", HttpStatus.NOT_FOUND));
	}

	/**
	 * Loads a member of the given organization or throws.
	 *
	 * @param organizationId organization id
	 * @param memberId organizer id
	 * @return member
	 */
	private Organizer requireMember(UUID organizationId, UUID memberId) {
		return organizerRepository.findByIdAndOrganizationId(memberId, organizationId)
				.orElseThrow(() -> new ApiException("MEMBER_NOT_FOUND", "Member not found", HttpStatus.NOT_FOUND));
	}

	/**
	 * Maps entity to response DTO.
	 *
	 * @param organization entity
	 * @return response
	 */
	private static OrganizationResponse toResponse(Organization organization) {
		return new OrganizationResponse(
				organization.getId(),
				organization.getName(),
				organization.getDescription(),
				organization.isActive(),
				organization.getCreatedAt());
	}

	/**
	 * Maps a member entity to response DTO.
	 *
	 * @param organizer entity
	 * @return response
	 */
	private static OrganizationMemberResponse toMemberResponse(Organizer organizer) {
		return new OrganizationMemberResponse(
				organizer.getId(),
				organizer.getEmail(),
				organizer.getDisplayName(),
				organizer.getOrganizationRole(),
				organizer.isActive(),
				organizer.getCreatedAt());
	}

	/**
	 * Trims blank text to null.
	 *
	 * @param value raw text
	 * @return trimmed text or null
	 */
	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
