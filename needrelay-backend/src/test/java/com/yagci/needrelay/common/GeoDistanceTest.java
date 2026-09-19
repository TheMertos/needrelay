package com.yagci.needrelay.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GeoDistanceTest {

	@Test
	void sameCoordinatesAreZero() {
		assertThat(GeoDistance.haversineKm(36.2, 37.1, 36.2, 37.1)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void oneDegreeLatitudeIsAboutHundredElevenKm() {
		BigDecimal distance = GeoDistance.haversineKm(0.0, 0.0, 1.0, 0.0);
		assertThat(distance.doubleValue()).isCloseTo(111.2, org.assertj.core.data.Offset.offset(1.0));
	}

	@Test
	void isSymmetric() {
		BigDecimal aToB = GeoDistance.haversineKm(36.2, 37.1, 33.5, 36.3);
		BigDecimal bToA = GeoDistance.haversineKm(33.5, 36.3, 36.2, 37.1);
		assertThat(aToB).isEqualByComparingTo(bToA);
	}
}
