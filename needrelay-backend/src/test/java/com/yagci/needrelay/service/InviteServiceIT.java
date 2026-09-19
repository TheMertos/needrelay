package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.repository.InviteRepository;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.web.dto.CreateInviteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InviteServiceIT {

	@Autowired
	private InviteService inviteService;

	@Autowired
	private InviteRepository inviteRepository;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Organizer orgAdmin;

	@BeforeEach
	void setUp() {
		inviteRepository.deleteAll();
		organizerRepository.deleteAll();
		organizationRepository.deleteAll();

		Organization organization = new Organization();
		organization.setName("Test Org");
		organizationRepository.save(organization);

		orgAdmin = new Organizer();
		orgAdmin.setEmail("org-admin@example.com");
		orgAdmin.setPasswordHash(passwordEncoder.encode("Password123!"));
		orgAdmin.setDisplayName("Org Admin");
		orgAdmin.setRole(OrganizerRole.ORGANIZER);
		orgAdmin.setOrganization(organization);
		orgAdmin.setOrganizationRole(OrganizationRole.ADMIN);
		orgAdmin.setActive(true);
		organizerRepository.save(orgAdmin);
	}

	@Test
	void createsAnInviteAndQueuesItsEmailForAsyncDelivery() {
		var response = inviteService.createInvite(
				orgAdmin.getId(), new CreateInviteRequest("member@example.com", 7, null, null));

		assertThat(response.id()).isNotNull();
		assertThat(response.email()).isEqualTo("member@example.com");
		assertThat(response.emailSentAt()).isNull();
		assertThat(inviteRepository.findById(response.id())).isPresent();
		assertThat(inviteRepository.findPendingEmail(5))
				.extracting(invite -> invite.getId())
				.containsExactly(response.id());
	}

	@Test
	void skipsEmailDeliveryWhenNoAddressIsGiven() {
		var response = inviteService.createInvite(
				orgAdmin.getId(), new CreateInviteRequest(null, 7, null, null));

		assertThat(inviteRepository.findById(response.id())).isPresent();
		assertThat(inviteRepository.findPendingEmail(5)).isEmpty();
	}
}
