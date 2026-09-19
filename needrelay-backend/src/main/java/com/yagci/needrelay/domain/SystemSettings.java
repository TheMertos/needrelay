package com.yagci.needrelay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Singleton row (id=1) holding platform-wide configuration.
 */
@Getter
@Setter
@Entity
@Table(name = "system_settings")
public class SystemSettings {

	@Id
	private Integer id;

	@Column(name = "default_language", nullable = false, length = 8)
	private String defaultLanguage;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/**
	 * Assigns timestamps before first persist.
	 */
	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	/**
	 * Refreshes updatedAt on each update.
	 */
	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
