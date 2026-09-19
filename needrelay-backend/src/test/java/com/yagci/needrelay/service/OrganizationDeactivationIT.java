package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.InviteRepository;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.RefreshTokenRepository;
import com.yagci.needrelay.web.dto.CreateOrganizationRequest;
import com.yagci.needrelay.web.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrganizationDeactivationIT {

	@Autowired
	private AuthService authService;

	@Autowired
	private AdminOrganizerService adminOrganizerService;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private InviteRepository inviteRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Organization organization;
	private Organizer member;

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAll();
		inviteRepository.deleteAll();
		organizerRepository.deleteAll();
		organizationRepository.deleteAll();

		var createdOrganization = adminOrganizerService.createOrganization(new CreateOrganizationRequest("Test Org", null));
		organization = organizationRepository.findById(createdOrganization.id()).orElseThrow();

		member = new Organizer();
		member.setEmail("member@example.com");
		member.setPasswordHash(passwordEncoder.encode("Password123!"));
		member.setDisplayName("Member");
		member.setRole(OrganizerRole.ORGANIZER);
		member.setOrganization(organization);
		member.setOrganizationRole(OrganizationRole.ADMIN);
		member.setActive(true);
		organizerRepository.save(member);
	}

	@Test
	void loginSucceedsWhileOrganizationIsActive() {
		var tokens = authService.login(new LoginRequest("member@example.com", "Password123!"));
		assertThat(tokens.accessToken()).isNotBlank();
	}

	@Test
	void loginFailsWithOrganizationDisabledAfterDeactivation() {
		adminOrganizerService.deactivateOrganization(organization.getId());

		assertThatThrownBy(() -> authService.login(new LoginRequest("member@example.com", "Password123!")))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> {
					ApiException apiException = (ApiException) ex;
					assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(apiException.getCode()).isEqualTo("ORGANIZATION_DISABLED");
				});
	}

	@Test
	void loginSucceedsAgainAfterReactivation() {
		adminOrganizerService.deactivateOrganization(organization.getId());
		adminOrganizerService.activateOrganization(organization.getId());

		var tokens = authService.login(new LoginRequest("member@example.com", "Password123!"));
		assertThat(tokens.accessToken()).isNotBlank();
	}
}
