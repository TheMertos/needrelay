package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.OfferStatus;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OfferRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import com.yagci.needrelay.web.dto.CreateOfferRequest;
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
	private ReliefRequestRepository reliefRequestRepository;

	@Autowired
	private NeedRepository needRepository;

	@Autowired
	private OfferRepository offerRepository;

	private Organizer organizer;
	private ReliefRequest request;
	private Need need;

	@BeforeEach
	void setUp() {
		offerRepository.deleteAll();
		needRepository.deleteAll();
		reliefRequestRepository.deleteAll();
		organizerRepository.deleteAll();

		organizer = new Organizer();
		organizer.setEmail("coord@example.com");
		organizer.setPasswordHash("hash");
		organizer.setDisplayName("Coordinator");
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizerRepository.save(organizer);

		request = new ReliefRequest();
		request.setOrganizer(organizer);
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
						"A", new BigDecimal("2"), "Ann", "Alpha", "+49001", "a@example.com", null));

		assertThat(offer.status()).isEqualTo(OfferStatus.PENDING);
		Need reloaded = needRepository.findById(need.getId()).orElseThrow();
		assertThat(reloaded.getQuantityOffered()).isEqualByComparingTo("0");
		assertThat(reloaded.getStatus()).isEqualTo(NeedStatus.OPEN);
	}

	@Test
	void receiveUpdatesNeedAndAllowsSurplus() {
		var offer = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"A", new BigDecimal("1"), "Ann", "Alpha", "+49001", "a@example.com", null));
		offerService.markComing(organizer.getId(), request.getId(), offer.id());
		offerService.markReceived(
				organizer.getId(),
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
						"A", new BigDecimal("1"), "Ann", "Alpha", "+49001", "a@example.com", null));
		var cancelledPending = offerService.cancelOffer(organizer.getId(), request.getId(), pending.id());
		assertThat(cancelledPending.status()).isEqualTo(OfferStatus.CANCELLED);

		var coming = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"B", new BigDecimal("1"), "Bob", "Beta", "+49002", "b@example.com", null));
		offerService.markComing(organizer.getId(), request.getId(), coming.id());
		var cancelledComing = offerService.cancelOffer(organizer.getId(), request.getId(), coming.id());
		assertThat(cancelledComing.status()).isEqualTo(OfferStatus.CANCELLED);

		Need reloaded = needRepository.findById(need.getId()).orElseThrow();
		assertThat(reloaded.getQuantityOffered()).isEqualByComparingTo("0");
		assertThat(reloaded.getStatus()).isEqualTo(NeedStatus.OPEN);
	}

	@Test
	void cancelReceivedFails() {
		var offer = offerService.createPublicOffer(
				need.getId(),
				new CreateOfferRequest(
						"A", new BigDecimal("1"), "Ann", "Alpha", "+49001", "a@example.com", null));
		offerService.markReceived(
				organizer.getId(),
				request.getId(),
				offer.id(),
				new ReceiveOfferRequest(new BigDecimal("1")));

		org.assertj.core.api.Assertions.assertThatThrownBy(
						() -> offerService.cancelOffer(organizer.getId(), request.getId(), offer.id()))
				.isInstanceOf(com.yagci.needrelay.exception.ApiException.class)
				.hasFieldOrPropertyWithValue("code", "OFFER_NOT_CANCELLABLE");
	}
}
