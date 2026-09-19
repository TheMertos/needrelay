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
 * One-time invite token to join an organization.
 */
@Getter
@Setter
@Entity
@Table(name = "invites")
public class Invite {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 64)
	private String token;

	@Column(length = 320)
	private String email;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_id", nullable = false)
	private Organizer createdBy;

	/**
	 * Organization the invited person will join. Null only for pre-migration legacy invites.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id")
	private Organization organization;

	@Enumerated(EnumType.STRING)
	@Column(name = "organization_role", nullable = false, length = 32)
	private OrganizationRole organizationRole = OrganizationRole.USER;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/**
	 * When the invite email was successfully delivered, or null if it has not been sent
	 * yet (no address given, still queued, or exhausted its retries).
	 */
	@Column(name = "email_sent_at")
	private Instant emailSentAt;

	/**
	 * Number of failed delivery attempts made by the async email dispatcher.
	 */
	@Column(name = "email_attempts", nullable = false)
	private int emailAttempts = 0;

	/**
	 * Error message from the most recent failed delivery attempt, or null.
	 */
	@Column(name = "email_error", length = 500)
	private String emailError;

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

	/**
	 * Returns whether the invite can still be consumed.
	 *
	 * @return true if unused and not expired
	 */
	public boolean isUsable() {
		return usedAt == null && Instant.now().isBefore(expiresAt);
	}
}
