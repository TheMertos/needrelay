package com.yagci.needrelay.domain;

import com.yagci.needrelay.common.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant organization that owns relief requests and has member accounts.
 */
@Getter
@Setter
@Entity
@Table(name = "organizations")
public class Organization {

	@Id
	private UUID id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

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
