package com.yagci.needrelay.domain;

import com.yagci.needrelay.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Public contact person for an organization.
 */
@Getter
@Setter
@Entity
@Table(name = "organizer_contacts")
public class OrganizerContact {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id")
	private Organization organization;

	/**
	 * Null for an organization-wide contact shown on every public page. When set, this
	 * contact is specific to one relief request and shown only on its public page.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "relief_request_id")
	private ReliefRequest reliefRequest;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = 200)
	private String role;

	@Column(nullable = false, length = 80)
	private String phone;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

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
