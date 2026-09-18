package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.repository.NeedRepository;
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
	private ReliefRequestRepository reliefRequestRepository;

	@Autowired
	private NeedRepository needRepository;

	@BeforeEach
	void setUp() {
		needRepository.deleteAll();
		reliefRequestRepository.deleteAll();
		organizerRepository.deleteAll();

		Organizer organizer = new Organizer();
		organizer.setEmail("disc@example.com");
		organizer.setPasswordHash("hash");
		organizer.setDisplayName("Aid Org");
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizerRepository.save(organizer);

		ReliefRequest active = new ReliefRequest();
		active.setOrganizer(organizer);
		active.setTitle("Active Camp");
		active.setDescription("d");
		active.setLocationLabel("Aleppo North");
		active.setLatitude(36.2);
		active.setLongitude(37.1);
		active.setPublicSlug("active-camp");
		active.setStatus(ReliefRequestStatus.ACTIVE);
		reliefRequestRepository.save(active);

		ReliefRequest archived = new ReliefRequest();
		archived.setOrganizer(organizer);
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
	void returnsOnlyActivePointsAndOpenOrPartialNeeds() {
		var result = publicDiscoveryService.getDiscovery();
		assertThat(result.points()).hasSize(1);
		assertThat(result.points().getFirst().publicSlug()).isEqualTo("active-camp");
		assertThat(result.points().getFirst().locationLabel()).isEqualTo("Aleppo North");
		assertThat(result.needs()).hasSize(1);
		assertThat(result.needs().getFirst().title()).isEqualTo("Water");
		assertThat(result.needs().getFirst().organizationName()).isEqualTo("Aid Org");
		assertThat(result.needs().getFirst().publicSlug()).isEqualTo("active-camp");
	}
}
