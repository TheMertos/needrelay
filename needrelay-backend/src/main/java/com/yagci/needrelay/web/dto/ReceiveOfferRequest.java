package com.yagci.needrelay.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Organizer confirmation of received offer quantity.
 *
 * @param quantityReceived actual quantity received
 */
public record ReceiveOfferRequest(
		@NotNull @DecimalMin(value = "0.0001") BigDecimal quantityReceived
) {
}
