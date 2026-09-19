package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.OfferStatus;
import com.yagci.needrelay.domain.Organization;
import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.ProviderType;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OfferRepository;
import com.yagci.needrelay.repository.OrganizationRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import com.yagci.needrelay.web.dto.CreateOfferRequest;
import com.yagci.needrelay.web.dto.OfferFilter;
import com.yagci.needrelay.web.dto.ReceiveOfferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OfferServiceIT {

	@Autowired
	private OfferService offerService;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private ReliefRequestRepository reliefRequestRepository;

	@Autowired
	private NeedRepository needRepository;

	@Autowired
	private OfferRepository offerRepository;

	private Organizer organizer;
	private Organization organization;
	private ReliefRequest request;
	private Need need;

	@BeforeEach
	void setUp() {
		offerRepository.deleteAll();
		needRepository.deleteAll();
		reliefRequestRepository.deleteAll();
		organizerRepository.deleteAll();
		organizationRepository.deleteAll();

		organization = new Organization();
		organization.setName("Org");
		organizationRepository.save(organization);

		organizer = new Organizer();
		organizer.setEmail("coord@example.com");
		organizer.setPasswordHash("hash");
		organizer.setDisplayName("Coordinator");
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizer.setOrganization(organization);
		organizer.setOrganizationRole(OrganizationRole.ADMIN);
		organizerRepository.save(organizer);

		request = new ReliefRequest();
		request.setOrganization(organization);
		request.setTitle("Antakya Support");
		request.setDescription("Test");
		request.setLocationLabel("Antakya");
		request.setLatitude(36.2);
		request.setLongitude(36.1);
		request.setPublicSlug("antakya-support");
		request.setStatus(ReliefRequestStatus.ACTIVE);
		reliefRequestRepository.save(request);

		need = new Need();
		need.setReliefRequest(request);
		need.setTitle("Excavator");
		need.setCategory(NeedCategory.MACHINERY);
		need.setQuantityRequired(new BigDecimal("2"));
		need.setQuantityOffered(BigDecimal.ZERO);
		need.setUnit("units");
		need.setPriority(NeedPriority.CRITICAL);
		need.setStatus(NeedStatus.OPEN);
		need = needRepository.save(need);
	}

	@Test
	void createLeavesNeedUnchangedAsPending() {
		var offer = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"A", ProviderType.ORGANIZATION, new BigDecimal("2"), "Ann", "Alpha", "+49001", "a@example.com", null, null, null));

		assertThat(offer.status()).isEqualTo(OfferStatus.PENDING);
		assertThat(offer.distanceKm()).isNull();
		Need reloaded = needRepository.findById(need.getId()).orElseThrow();
		assertThat(reloaded.getQuantityOffered()).isEqualByComparingTo("0");
		assertThat(reloaded.getStatus()).isEqualTo(NeedStatus.OPEN);
	}

	@Test
	void createComputesDistanceWhenLocationShared() {
		var offer = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"A", ProviderType.ORGANIZATION, new BigDecimal("2"), "Ann", "Alpha", "+49001", "a@example.com", null, 36.3, 36.1));

		assertThat(offer.distanceKm()).isNotNull();
		assertThat(offer.distanceKm().doubleValue()).isCloseTo(11.1, org.assertj.core.data.Offset.offset(0.5));
	}

	@Test
	void receiveUpdatesNeedAndAllowsSurplus() {
		var offer = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"A", ProviderType.ORGANIZATION, new BigDecimal("1"), "Ann", "Alpha", "+49001", "a@example.com", null, null, null));
		offerService.markComing(organization.getId(), request.getId(), offer.id());
		offerService.markReceived(
				organization.getId(),
				request.getId(),
				offer.id(),
				new ReceiveOfferRequest(new BigDecimal("3")));

		Need reloaded = needRepository.findById(need.getId()).orElseThrow();
		assertThat(reloaded.getQuantityOffered()).isEqualByComparingTo("3");
		assertThat(reloaded.getStatus()).isEqualTo(NeedStatus.COVERED);
		assertThat(reloaded.remaining()).isEqualByComparingTo("0");
	}

	@Test
	void cancelPendingAndComingLeavesNeedUnchanged() {
		var pending = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"A", ProviderType.ORGANIZATION, new BigDecimal("1"), "Ann", "Alpha", "+49001", "a@example.com", null, null, null));
		var cancelledPending = offerService.cancelOffer(organization.getId(), request.getId(), pending.id());
		assertThat(cancelledPending.status()).isEqualTo(OfferStatus.CANCELLED);

		var coming = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"B", ProviderType.PERSON, new BigDecimal("1"), "Bob", "Beta", "+49002", "b@example.com", null, null, null));
		offerService.markComing(organization.getId(), request.getId(), coming.id());
		var cancelledComing = offerService.cancelOffer(organization.getId(), request.getId(), coming.id());
		assertThat(cancelledComing.status()).isEqualTo(OfferStatus.CANCELLED);

		Need reloaded = needRepository.findById(need.getId()).orElseThrow();
		assertThat(reloaded.getQuantityOffered()).isEqualByComparingTo("0");
		assertThat(reloaded.getStatus()).isEqualTo(NeedStatus.OPEN);
	}

	@Test
	void listOffersFiltersByProviderTypeAndSortsByProviderName() {
		offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"Zebra Org", ProviderType.ORGANIZATION, new BigDecimal("1"), "Zed", "Z",
						"+1", "z@example.com", null, null, null));
		offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"Amy", ProviderType.PERSON, new BigDecimal("1"), "Amy", "A",
						"+2", "amy@example.com", null, null, null));
		offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"Apex Org", ProviderType.ORGANIZATION, new BigDecimal("1"), "Apex", "A",
						"+3", "apex@example.com", null, null, null));

		var orgOffers = offerService.listOffers(
				organization.getId(), request.getId(), 0, 20, "providerName,asc",
				new OfferFilter(null, null, ProviderType.ORGANIZATION, null, null, null, null, null));

		assertThat(orgOffers.items()).hasSize(2);
		assertThat(orgOffers.items().get(0).providerName()).isEqualTo("Apex Org");
		assertThat(orgOffers.items().get(1).providerName()).isEqualTo("Zebra Org");
		assertThat(orgOffers.items()).allMatch(o -> o.providerType() == ProviderType.ORGANIZATION);
	}

	@Test
	void listOffersSearchesAcrossNameAndContactFields() {
		offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"Zebra Org", ProviderType.ORGANIZATION, new BigDecimal("1"), "Zed", "Z",
						"+1", "z@example.com", null, null, null));
		offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"Amy", ProviderType.PERSON, new BigDecimal("1"), "Amy", "A",
						"+2", "amy@unique-domain.com", null, null, null));

		var result = offerService.listOffers(
				organization.getId(), request.getId(), 0, 20, "createdAt,desc",
				new OfferFilter(null, null, null, "unique-domain", null, null, null, null));

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().providerName()).isEqualTo("Amy");
	}

	@Test
	void listOffersScopesToASingleNeedWhenNeedIdFilterIsSet() {
		Need otherNeed = new Need();
		otherNeed.setReliefRequest(request);
		otherNeed.setTitle("Blankets");
		otherNeed.setCategory(NeedCategory.SHELTER);
		otherNeed.setQuantityRequired(new BigDecimal("10"));
		otherNeed.setQuantityOffered(BigDecimal.ZERO);
		otherNeed.setUnit("pcs");
		otherNeed.setPriority(NeedPriority.NORMAL);
		otherNeed.setStatus(NeedStatus.OPEN);
		needRepository.save(otherNeed);

		offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"On need A", ProviderType.PERSON, new BigDecimal("1"), "A", "A",
						"+1", "a@example.com", null, null, null));
		offerService.createPublicOffer(
				otherNeed.getId(),
				new CreateOfferRequest(
						"On need B", ProviderType.PERSON, new BigDecimal("1"), "B", "B",
						"+2", "b@example.com", null, null, null));

		var result = offerService.listOffers(
				organization.getId(), request.getId(), 0, 20, "createdAt,desc",
				new OfferFilter(otherNeed.getId(), null, null, null, null, null, null, null));

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().providerName()).isEqualTo("On need B");
	}

	@Test
	void createRejectsOfferOnArchivedRequest() {
		request.setStatus(ReliefRequestStatus.ARCHIVED);
		reliefRequestRepository.save(request);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> offerService.createPublicOffer(
						need.getId(),
						new CreateOfferRequest(
								"A", ProviderType.ORGANIZATION, new BigDecimal("1"), "Ann", "Alpha",
								"+49001", "a@example.com", null, null, null)))
				.isInstanceOf(com.yagci.needrelay.exception.ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RELIEF_REQUEST_ARCHIVED");
	}

	@Test
	void cancelReceivedFails() {
		var offer = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"A", ProviderType.ORGANIZATION, new BigDecimal("1"), "Ann", "Alpha", "+49001", "a@example.com", null, null, null));
		offerService.markReceived(
				organization.getId(),
				request.getId(),
				offer.id(),
				new ReceiveOfferRequest(new BigDecimal("1")));

		org.assertj.core.api.Assertions.assertThatThrownBy(
						() -> offerService.cancelOffer(organization.getId(), request.getId(), offer.id()))
				.isInstanceOf(com.yagci.needrelay.exception.ApiException.class)
				.hasFieldOrPropertyWithValue("code", "OFFER_NOT_CANCELLABLE");
	}
}
