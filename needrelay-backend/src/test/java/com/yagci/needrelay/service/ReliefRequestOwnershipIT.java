package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizationRepository;
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
	private OrganizationRepository organizationRepository;

	@Autowired
	private ReliefRequestRepository reliefRequestRepository;

	private Organization otherOrganization;
	private ReliefRequest request;

	@BeforeEach
	void setUp() {
		reliefRequestRepository.deleteAll();
		organizerRepository.deleteAll();
		organizationRepository.deleteAll();

		Organization ownerOrganization = saveOrganization("Owner Org");
		otherOrganization = saveOrganization("Other Org");
		saveOrganizer("owner@example.com", "Owner", ownerOrganization);
		saveOrganizer("other@example.com", "Other", otherOrganization);

		request = new ReliefRequest();
		request.setOrganization(ownerOrganization);
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
		assertThatThrownBy(() -> reliefRequestService.getOwned(otherOrganization.getId(), request.getId()))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	/**
	 * Persists a minimal organization.
	 *
	 * @param name organization name
	 * @return saved organization
	 */
	private Organization saveOrganization(String name) {
		Organization organization = new Organization();
		organization.setName(name);
		return organizationRepository.save(organization);
	}

	/**
	 * Persists a minimal organizer belonging to the given organization.
	 *
	 * @param email email
	 * @param name display name
	 * @param organization owning organization
	 * @return saved organizer
	 */
	private Organizer saveOrganizer(String email, String name, Organization organization) {
		Organizer organizer = new Organizer();
		organizer.setEmail(email);
		organizer.setPasswordHash("hash");
		organizer.setDisplayName(name);
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizer.setOrganization(organization);
		organizer.setOrganizationRole(OrganizationRole.ADMIN);
		return organizerRepository.save(organizer);
	}
}
