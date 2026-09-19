package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.Offer;
import com.yagci.needrelay.web.dto.OfferFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds combinable {@link Specification}s for the organizer offers table.
 */
public final class OfferSpecifications {

	private OfferSpecifications() {
	}

	/**
	 * Combines a relief request scope with optional, combinable filters.
	 *
	 * @param reliefRequestId parent relief request id
	 * @param filter optional filter values (null fields are ignored)
	 * @return specification matching all given criteria
	 */
	public static Specification<Offer> forRequest(UUID reliefRequestId, OfferFilter filter) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("need").get("reliefRequest").get("id"), reliefRequestId));

			if (filter != null) {
				if (filter.needId() != null) {
					predicates.add(cb.equal(root.get("need").get("id"), filter.needId()));
				}
				if (filter.status() != null) {
					predicates.add(cb.equal(root.get("status"), filter.status()));
				}
				if (filter.providerType() != null) {
					predicates.add(cb.equal(root.get("providerType"), filter.providerType()));
				}
				if (filter.q() != null && !filter.q().isBlank()) {
					String like = "%" + filter.q().trim().toLowerCase() + "%";
					predicates.add(cb.or(
							cb.like(cb.lower(root.get("providerName")), like),
							cb.like(cb.lower(root.get("firstName")), like),
							cb.like(cb.lower(root.get("lastName")), like),
							cb.like(cb.lower(root.get("phone")), like),
							cb.like(cb.lower(root.get("email")), like)));
				}
				if (filter.minQuantity() != null) {
					predicates.add(cb.ge(root.get("quantity"), filter.minQuantity()));
				}
				if (filter.maxQuantity() != null) {
					predicates.add(cb.le(root.get("quantity"), filter.maxQuantity()));
				}
				if (filter.minDistanceKm() != null) {
					predicates.add(cb.ge(root.get("distanceKm"), filter.minDistanceKm()));
				}
				if (filter.maxDistanceKm() != null) {
					predicates.add(cb.le(root.get("distanceKm"), filter.maxDistanceKm()));
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
