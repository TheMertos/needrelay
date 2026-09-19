package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.repository.InviteRepository;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.web.dto.CreateInviteRequest;
import com.yagci.needrelay.web.dto.InviteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")
class InviteEmailDispatchServiceIT {

	@Autowired
	private InviteService inviteService;

	@Autowired
	private InviteEmailDispatchService dispatchService;

	@Autowired
	private InviteRepository inviteRepository;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private EmailService emailService;

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
	void sendsAQueuedInviteEmailAndMarksItDelivered() {
		InviteResponse invite = inviteService.createInvite(
				orgAdmin.getId(), new CreateInviteRequest("member@example.com", 7, null, null));

		dispatchService.dispatchPending();

		Mockito.verify(emailService).sendInvite(anyString(), anyString());
		assertThat(inviteRepository.findById(invite.id()).orElseThrow().getEmailSentAt()).isNotNull();
		assertThat(inviteRepository.findPendingEmail(5)).isEmpty();
	}

	@Test
	void recordsTheFailureAndLeavesTheInviteQueuedForRetry() {
		Mockito.doThrow(new IllegalStateException("Resend is down"))
				.when(emailService).sendInvite(anyString(), anyString());

		InviteResponse invite = inviteService.createInvite(
				orgAdmin.getId(), new CreateInviteRequest("member@example.com", 7, null, null));

		dispatchService.dispatchPending();

		var stored = inviteRepository.findById(invite.id()).orElseThrow();
		assertThat(stored.getEmailSentAt()).isNull();
		assertThat(stored.getEmailAttempts()).isEqualTo(1);
		assertThat(stored.getEmailError()).contains("Resend is down");
		assertThat(inviteRepository.findPendingEmail(5)).extracting(i -> i.getId()).containsExactly(invite.id());
	}
}
