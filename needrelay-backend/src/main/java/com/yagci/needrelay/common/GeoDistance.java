package com.yagci.needrelay.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Great-circle distance between two coordinates.
 */
public final class GeoDistance {

	private static final double EARTH_RADIUS_KM = 6371.0;

	private GeoDistance() {
	}

	/**
	 * Computes the Haversine distance between two points, rounded to one decimal km.
	 *
	 * @param lat1 first point latitude
	 * @param lon1 first point longitude
	 * @param lat2 second point latitude
	 * @param lon2 second point longitude
	 * @return distance in kilometers, rounded to one decimal place
	 */
	public static BigDecimal haversineKm(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double km = EARTH_RADIUS_KM * c;
		return BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP);
	}
}
