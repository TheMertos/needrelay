package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PublicDiscoveryServiceIT {

	@Autowired
	private PublicDiscoveryService publicDiscoveryService;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private ReliefRequestRepository reliefRequestRepository;

	@Autowired
	private NeedRepository needRepository;

	@BeforeEach
	void setUp() {
		needRepository.deleteAll();
		reliefRequestRepository.deleteAll();
		organizerRepository.deleteAll();
		organizationRepository.deleteAll();

		Organization organization = new Organization();
		organization.setName("Aid Org");
		organizationRepository.save(organization);

		Organizer organizer = new Organizer();
		organizer.setEmail("disc@example.com");
		organizer.setPasswordHash("hash");
		organizer.setDisplayName("Aid Org");
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizer.setOrganization(organization);
		organizer.setOrganizationRole(OrganizationRole.ADMIN);
		organizerRepository.save(organizer);

		ReliefRequest active = new ReliefRequest();
		active.setOrganization(organization);
		active.setTitle("Active Camp");
		active.setDescription("d");
		active.setLocationLabel("Aleppo North");
		active.setLatitude(36.2);
		active.setLongitude(37.1);
		active.setPublicSlug("active-camp");
		active.setStatus(ReliefRequestStatus.ACTIVE);
		reliefRequestRepository.save(active);

		ReliefRequest archived = new ReliefRequest();
		archived.setOrganization(organization);
		archived.setTitle("Archived Camp");
		archived.setDescription("d");
		archived.setLocationLabel("Elsewhere");
		archived.setLatitude(1);
		archived.setLongitude(2);
		archived.setPublicSlug("archived-camp");
		archived.setStatus(ReliefRequestStatus.ARCHIVED);
		reliefRequestRepository.save(archived);

		Need openNeed = new Need();
		openNeed.setReliefRequest(active);
		openNeed.setTitle("Water");
		openNeed.setCategory(NeedCategory.WATER);
		openNeed.setQuantityRequired(new BigDecimal("100"));
		openNeed.setQuantityOffered(BigDecimal.ZERO);
		openNeed.setUnit("L");
		openNeed.setPriority(NeedPriority.CRITICAL);
		openNeed.setStatus(NeedStatus.OPEN);
		needRepository.save(openNeed);

		Need covered = new Need();
		covered.setReliefRequest(active);
		covered.setTitle("Blankets");
		covered.setCategory(NeedCategory.SHELTER);
		covered.setQuantityRequired(new BigDecimal("10"));
		covered.setQuantityOffered(new BigDecimal("10"));
		covered.setUnit("pcs");
		covered.setPriority(NeedPriority.LOW);
		covered.setStatus(NeedStatus.COVERED);
		needRepository.save(covered);

		Need closedNeed = new Need();
		closedNeed.setReliefRequest(active);
		closedNeed.setTitle("Old");
		closedNeed.setCategory(NeedCategory.OTHER);
		closedNeed.setQuantityRequired(new BigDecimal("1"));
		closedNeed.setQuantityOffered(BigDecimal.ZERO);
		closedNeed.setUnit("x");
		closedNeed.setPriority(NeedPriority.NORMAL);
		closedNeed.setStatus(NeedStatus.CLOSED);
		needRepository.save(closedNeed);

		Need onArchivedRequest = new Need();
		onArchivedRequest.setReliefRequest(archived);
		onArchivedRequest.setTitle("Should hide");
		onArchivedRequest.setCategory(NeedCategory.FOOD);
		onArchivedRequest.setQuantityRequired(new BigDecimal("5"));
		onArchivedRequest.setQuantityOffered(BigDecimal.ZERO);
		onArchivedRequest.setUnit("kg");
		onArchivedRequest.setPriority(NeedPriority.HIGH);
		onArchivedRequest.setStatus(NeedStatus.OPEN);
		needRepository.save(onArchivedRequest);
	}

	@Test
	void returnsOnlyActivePoints() {
		var result = publicDiscoveryService.getDiscovery(null, 0, 20);
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().publicSlug()).isEqualTo("active-camp");
		assertThat(result.items().getFirst().locationLabel()).isEqualTo("Aleppo North");
		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.totalPages()).isEqualTo(1);
	}

	@Test
	void searchMatchesLocationLabel() {
		var result = publicDiscoveryService.getDiscovery("aleppo", 0, 20);
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().publicSlug()).isEqualTo("active-camp");
	}

	@Test
	void searchMatchesNeedTitleOnDiscoverableNeeds() {
		var matches = publicDiscoveryService.getDiscovery("water", 0, 20);
		assertThat(matches.items()).hasSize(1);
		assertThat(matches.items().getFirst().publicSlug()).isEqualTo("active-camp");

		var noMatches = publicDiscoveryService.getDiscovery("old", 0, 20);
		assertThat(noMatches.items()).isEmpty();
	}

	@Test
	void searchWithNoMatchReturnsEmptyPage() {
		var result = publicDiscoveryService.getDiscovery("no-such-place", 0, 20);
		assertThat(result.items()).isEmpty();
		assertThat(result.totalElements()).isEqualTo(0);
	}

	@Test
	void pagesPointsCappedAtFiftyPerPage() {
		for (int i = 0; i < 5; i++) {
			ReliefRequest extra = new ReliefRequest();
			extra.setOrganization(reliefRequestRepository.findByPublicSlug("active-camp").orElseThrow().getOrganization());
			extra.setTitle("Extra Point " + i);
			extra.setDescription("d");
			extra.setLocationLabel("Somewhere " + i);
			extra.setLatitude(1);
			extra.setLongitude(2);
			extra.setPublicSlug("extra-point-" + i);
			extra.setStatus(ReliefRequestStatus.ACTIVE);
			reliefRequestRepository.save(extra);
		}

		var firstPage = publicDiscoveryService.getDiscovery(null, 0, 100);
		assertThat(firstPage.items()).hasSize(6);
		assertThat(firstPage.size()).isEqualTo(50);
		assertThat(firstPage.totalElements()).isEqualTo(6);
		assertThat(firstPage.totalPages()).isEqualTo(1);

		var firstOfTwo = publicDiscoveryService.getDiscovery(null, 0, 4);
		assertThat(firstOfTwo.items()).hasSize(4);
		assertThat(firstOfTwo.totalPages()).isEqualTo(2);
	}

	@Test
	void pagesPointNeedsCappedAtTwentyPerPage() {
		ReliefRequest active = reliefRequestRepository.findByPublicSlug("active-camp").orElseThrow();
		for (int i = 0; i < 25; i++) {
			Need extra = new Need();
			extra.setReliefRequest(active);
			extra.setTitle("Extra " + i);
			extra.setCategory(NeedCategory.SUPPLIES);
			extra.setQuantityRequired(new BigDecimal("1"));
			extra.setQuantityOffered(BigDecimal.ZERO);
			extra.setUnit("x");
			extra.setPriority(NeedPriority.NORMAL);
			extra.setStatus(NeedStatus.OPEN);
			needRepository.save(extra);
		}

		var firstPage = publicDiscoveryService.getPointNeeds(active.getId(), 0, 100);
		assertThat(firstPage.items()).hasSize(20);
		assertThat(firstPage.size()).isEqualTo(20);
		assertThat(firstPage.totalElements()).isEqualTo(26);
		assertThat(firstPage.totalPages()).isEqualTo(2);

		var secondPage = publicDiscoveryService.getPointNeeds(active.getId(), 1, 20);
		assertThat(secondPage.items()).hasSize(6);
	}
}
