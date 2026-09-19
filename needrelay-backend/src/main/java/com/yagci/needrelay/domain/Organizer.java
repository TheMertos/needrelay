package com.yagci.needrelay.domain;

import com.yagci.needrelay.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Login account for a person. Platform-wide {@link OrganizerRole#ADMIN} accounts have no
 * organization; regular accounts belong to exactly one {@link Organization} with a per-org
 * {@link OrganizationRole}.
 */
@Getter
@Setter
@Entity
@Table(name = "organizers")
public class Organizer {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 200)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OrganizerRole role;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id")
	private Organization organization;

	@Enumerated(EnumType.STRING)
	@Column(name = "organization_role", length = 32)
	private OrganizationRole organizationRole;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/**
	 * Assigns UUID7 and creation timestamp before first persist.
	 */
	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UuidV7.generate();
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
