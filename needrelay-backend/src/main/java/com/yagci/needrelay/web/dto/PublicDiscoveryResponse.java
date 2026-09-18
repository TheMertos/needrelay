package com.yagci.needrelay.web.dto;

import java.util.List;

/**
 * Public discovery payload for map pins and open needs.
 *
 * @param points ACTIVE relief request pins
 * @param needs open/partial needs on those requests
 */
public record PublicDiscoveryResponse(
		List<DiscoveryPointResponse> points,
		List<DiscoveryNeedResponse> needs
) {
}
