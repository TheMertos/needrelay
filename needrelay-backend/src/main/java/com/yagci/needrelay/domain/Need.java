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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Individual resource need within a relief request.
 */
@Getter
@Setter
@Entity
@Table(name = "needs")
public class Need {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "relief_request_id", nullable = false)
	private ReliefRequest reliefRequest;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private NeedCategory category;

	@Column(name = "quantity_required", nullable = false, precision = 19, scale = 4)
	private BigDecimal quantityRequired;

	@Column(name = "quantity_offered", nullable = false, precision = 19, scale = 4)
	private BigDecimal quantityOffered = BigDecimal.ZERO;

	@Column(nullable = false, length = 64)
	private String unit;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private NeedPriority priority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private NeedStatus status = NeedStatus.OPEN;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/**
	 * Assigns UUID7 and timestamps before first persist.
	 */
	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UuidV7.generate();
		}
		if (quantityOffered == null) {
			quantityOffered = BigDecimal.ZERO;
		}
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

	/**
	 * Computes remaining quantity still needed (never negative).
	 *
	 * @return max(0, required − offered/received)
	 */
	public BigDecimal remaining() {
		BigDecimal raw = quantityRequired.subtract(quantityOffered);
		return raw.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : raw;
	}

	/**
	 * Recomputes status from quantities unless manually closed.
	 */
	public void refreshCoverageStatus() {
		if (status == NeedStatus.CLOSED) {
			return;
		}
		int cmp = quantityOffered.compareTo(BigDecimal.ZERO);
		int full = quantityOffered.compareTo(quantityRequired);
		if (cmp == 0) {
			status = NeedStatus.OPEN;
		}
		else if (full >= 0) {
			status = NeedStatus.COVERED;
		}
		else {
			status = NeedStatus.PARTIALLY_COVERED;
		}
	}
}
