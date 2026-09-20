package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.RefreshTokenRepository;
import com.yagci.needrelay.web.dto.CreateOrganizationRequest;
import com.yagci.needrelay.web.dto.OrganizationResponse;
import com.yagci.needrelay.web.dto.OrganizerResponse;
import com.yagci.needrelay.web.dto.UpdateOrganizationRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin account management: list/ban/unban organizers, create/list organizations.
 */
@Service
@AllArgsConstructor
public class AdminOrganizerService {

	private final OrganizerRepository organizerRepository;
	private final OrganizationRepository organizationRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	/**
	 * Creates a new, empty organization.
	 *
	 * @param request name/description
	 * @return created organization
	 */
	@Transactional
	public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
		Organization organization = new Organization();
		organization.setName(request.name().trim());
		organization.setDescription(blankToNull(request.description()));
		organizationRepository.save(organization);
		return toOrganizationResponse(organization);
	}

	/**
	 * Lists all organizations, newest first.
	 *
	 * @return organizations
	 */
	@Transactional(readOnly = true)
	public List<OrganizationResponse> listOrganizations() {
		return organizationRepository.findAll().stream()
				.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
				.map(AdminOrganizerService::toOrganizationResponse)
				.toList();
	}

	/**
	 * Updates an organization's name/description.
	 *
	 * @param organizationId target organization
	 * @param request name/description
	 * @return updated organization
	 */
	@Transactional
	public OrganizationResponse updateOrganization(UUID organizationId, UpdateOrganizationRequest request) {
		Organization organization = requireOrganization(organizationId);
		organization.setName(request.name().trim());
		organization.setDescription(blankToNull(request.description()));
		organizationRepository.save(organization);
		return toOrganizationResponse(organization);
	}

	/**
	 * Deactivates an organization and revokes every member's active sessions. Members can
	 * no longer sign in or refresh a session while the organization is deactivated.
	 *
	 * @param organizationId target organization
	 * @return updated organization
	 */
	@Transactional
	public OrganizationResponse deactivateOrganization(UUID organizationId) {
		Organization organization = requireOrganization(organizationId);
		organization.setActive(false);
		organizationRepository.save(organization);
		Instant now = Instant.now();
		organizerRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId)
				.forEach(member -> refreshTokenRepository.revokeAllActiveForOrganizer(member.getId(), now));
		return toOrganizationResponse(organization);
	}

	/**
	 * Reactivates an organization, allowing its members to sign in again.
	 *
	 * @param organizationId target organization
	 * @return updated organization
	 */
	@Transactional
	public OrganizationResponse activateOrganization(UUID organizationId) {
		Organization organization = requireOrganization(organizationId);
		organization.setActive(true);
		organizationRepository.save(organization);
		return toOrganizationResponse(organization);
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
	 * Maps entity to response DTO.
	 *
	 * @param organization entity
	 * @return response
	 */
	private static OrganizationResponse toOrganizationResponse(Organization organization) {
		return new OrganizationResponse(
				organization.getId(),
				organization.getName(),
				organization.getDescription(),
				organization.isActive(),
				organization.getCreatedAt());
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
	 * Changes an organization member's role. Rejects demoting the organization's last
	 * remaining admin, and targets with no organization (platform admin accounts).
	 *
	 * @param organizerId target organizer
	 * @param newRole new organization role
	 * @return updated organizer
	 */
	@Transactional
	public OrganizerResponse updateOrganizationRole(UUID organizerId, OrganizationRole newRole) {
		Organizer target = requireOrganizer(organizerId);
		if (target.getOrganization() == null) {
			throw new ApiException(
					"NOT_ORGANIZATION_MEMBER", "This account has no organization", HttpStatus.BAD_REQUEST);
		}
		if (target.getOrganizationRole() == OrganizationRole.ADMIN
				&& newRole == OrganizationRole.USER
				&& organizerRepository.countByOrganizationIdAndOrganizationRole(
						target.getOrganization().getId(), OrganizationRole.ADMIN) <= 1) {
			throw new ApiException(
					"LAST_ADMIN", "Cannot demote the organization's last remaining admin", HttpStatus.CONFLICT);
		}
		target.setOrganizationRole(newRole);
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
