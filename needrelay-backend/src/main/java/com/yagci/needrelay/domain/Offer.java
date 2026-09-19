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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Anonymous resource offer against a need.
 */
@Getter
@Setter
@Entity
@Table(name = "offers")
public class Offer {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "need_id", nullable = false)
	private Need need;

	@Column(name = "provider_name", nullable = false, length = 200)
	private String providerName;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal quantity;

	@Column(name = "quantity_received", precision = 19, scale = 4)
	private BigDecimal quantityReceived;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OfferStatus status = OfferStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_type", nullable = false, length = 32)
	private ProviderType providerType;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, length = 80)
	private String phone;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "distance_km", precision = 10, scale = 1)
	private BigDecimal distanceKm;

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
		if (status == null) {
			status = OfferStatus.PENDING;
		}
	}
}
