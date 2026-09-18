package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ReliefRequestOwnershipIT {

	@Autowired
	private ReliefRequestService reliefRequestService;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private ReliefRequestRepository reliefRequestRepository;

	private Organizer other;
	private ReliefRequest request;

	@BeforeEach
	void setUp() {
		reliefRequestRepository.deleteAll();
		organizerRepository.deleteAll();

		Organizer owner = saveOrganizer("owner@example.com", "Owner");
		other = saveOrganizer("other@example.com", "Other");

		request = new ReliefRequest();
		request.setOrganizer(owner);
		request.setTitle("Owned request");
		request.setDescription("desc");
		request.setLocationLabel("Hatay");
		request.setLatitude(36.2);
		request.setLongitude(36.1);
		request.setPublicSlug("owned-request");
		request.setStatus(ReliefRequestStatus.ACTIVE);
		request = reliefRequestRepository.save(request);
	}

	@Test
	void forbidsAccessToForeignReliefRequest() {
		assertThatThrownBy(() -> reliefRequestService.getOwned(other.getId(), request.getId()))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	/**
	 * Persists a minimal organizer.
	 *
	 * @param email email
	 * @param name display name
	 * @return saved organizer
	 */
	private Organizer saveOrganizer(String email, String name) {
		Organizer organizer = new Organizer();
		organizer.setEmail(email);
		organizer.setPasswordHash("hash");
		organizer.setDisplayName(name);
		organizer.setRole(OrganizerRole.ORGANIZER);
		return organizerRepository.save(organizer);
	}
}
