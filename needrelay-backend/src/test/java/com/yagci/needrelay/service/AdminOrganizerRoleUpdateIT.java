package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.web.dto.CreateOrganizationRequest;
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
class AdminOrganizerRoleUpdateIT {

	@Autowired
	private AdminOrganizerService adminOrganizerService;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Organization organization;
	private Organizer admin;
	private Organizer member;

	@BeforeEach
	void setUp() {
		organizerRepository.deleteAll();
		organizationRepository.deleteAll();

		var createdOrganization = adminOrganizerService.createOrganization(new CreateOrganizationRequest("Test Org", null));
		organization = organizationRepository.findById(createdOrganization.id()).orElseThrow();

		admin = newMember("admin@example.com", OrganizationRole.ADMIN);
		member = newMember("member@example.com", OrganizationRole.USER);
	}

	private Organizer newMember(String email, OrganizationRole organizationRole) {
		Organizer organizer = new Organizer();
		organizer.setEmail(email);
		organizer.setPasswordHash(passwordEncoder.encode("Password123!"));
		organizer.setDisplayName(email);
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizer.setOrganization(organization);
		organizer.setOrganizationRole(organizationRole);
		organizer.setActive(true);
		return organizerRepository.save(organizer);
	}

	@Test
	void promotesUserToAdmin() {
		var updated = adminOrganizerService.updateOrganizationRole(member.getId(), OrganizationRole.ADMIN);

		assertThat(updated.organizationRole()).isEqualTo(OrganizationRole.ADMIN);
		Organizer reloaded = organizerRepository.findById(member.getId()).orElseThrow();
		assertThat(reloaded.getOrganizationRole()).isEqualTo(OrganizationRole.ADMIN);
	}

	@Test
	void rejectsDemotingTheLastRemainingAdmin() {
		assertThatThrownBy(() -> adminOrganizerService.updateOrganizationRole(admin.getId(), OrganizationRole.USER))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> {
					ApiException apiException = (ApiException) ex;
					assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(apiException.getCode()).isEqualTo("LAST_ADMIN");
				});
	}

	@Test
	void allowsDemotingAnAdminWhenAnotherAdminRemains() {
		adminOrganizerService.updateOrganizationRole(member.getId(), OrganizationRole.ADMIN);

		var updated = adminOrganizerService.updateOrganizationRole(admin.getId(), OrganizationRole.USER);

		assertThat(updated.organizationRole()).isEqualTo(OrganizationRole.USER);
	}

	@Test
	void rejectsChangingRoleForAnAccountWithNoOrganization() {
		Organizer platformAdmin = new Organizer();
		platformAdmin.setEmail("platform-admin@example.com");
		platformAdmin.setPasswordHash(passwordEncoder.encode("Password123!"));
		platformAdmin.setDisplayName("Platform Admin");
		platformAdmin.setRole(OrganizerRole.ADMIN);
		platformAdmin.setActive(true);
		organizerRepository.save(platformAdmin);

		assertThatThrownBy(() -> adminOrganizerService.updateOrganizationRole(platformAdmin.getId(), OrganizationRole.ADMIN))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> {
					ApiException apiException = (ApiException) ex;
					assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(apiException.getCode()).isEqualTo("NOT_ORGANIZATION_MEMBER");
				});
	}
}
