package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import com.yagci.needrelay.web.dto.UpdateNeedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class NeedServiceIT {

	@Autowired
	private NeedService needService;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private ReliefRequestRepository reliefRequestRepository;

	@Autowired
	private NeedRepository needRepository;

	private Organizer organizer;
	private ReliefRequest request;
	private Need need;

	@BeforeEach
	void setUp() {
		needRepository.deleteAll();
		reliefRequestRepository.deleteAll();
		organizerRepository.deleteAll();

		organizer = new Organizer();
		organizer.setEmail("owner@example.com");
		organizer.setPasswordHash("hash");
		organizer.setDisplayName("Owner");
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizerRepository.save(organizer);

		request = new ReliefRequest();
		request.setOrganizer(organizer);
		request.setTitle("Req");
		request.setDescription("d");
		request.setLocationLabel("Loc");
		request.setLatitude(1);
		request.setLongitude(2);
		request.setPublicSlug("req-edit");
		request.setStatus(ReliefRequestStatus.ACTIVE);
		reliefRequestRepository.save(request);

		need = new Need();
		need.setReliefRequest(request);
		need.setTitle("Water");
		need.setCategory(NeedCategory.WATER);
		need.setQuantityRequired(new BigDecimal("500"));
		need.setQuantityOffered(new BigDecimal("200"));
		need.setUnit("L");
		need.setPriority(NeedPriority.HIGH);
		need.setStatus(NeedStatus.PARTIALLY_COVERED);
		need = needRepository.save(need);
	}

	@Test
	void allowsReducingRequiredDownToOffered() {
		var updated = needService.update(
				organizer.getId(),
				request.getId(),
				need.getId(),
				new UpdateNeedRequest(
						"Water",
						null,
						NeedCategory.WATER,
						new BigDecimal("200"),
						"L",
						NeedPriority.HIGH));
		assertThat(updated.quantityRequired()).isEqualByComparingTo("200");
		assertThat(updated.remaining()).isEqualByComparingTo("0");
		assertThat(updated.status()).isEqualTo(NeedStatus.COVERED);
	}

	@Test
	void rejectsRequiredBelowOffered() {
		assertThatThrownBy(() -> needService.update(
				organizer.getId(),
				request.getId(),
				need.getId(),
				new UpdateNeedRequest(
						"Water",
						null,
						NeedCategory.WATER,
						new BigDecimal("100"),
						"L",
						NeedPriority.HIGH)))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void closesCoveredNeed() {
		need.setQuantityRequired(new BigDecimal("200"));
		need.setStatus(NeedStatus.COVERED);
		needRepository.save(need);

		var closed = needService.close(organizer.getId(), request.getId(), need.getId());
		assertThat(closed.status()).isEqualTo(NeedStatus.CLOSED);
	}
}
