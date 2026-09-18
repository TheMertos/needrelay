package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create need payload.
 *
 * @param title title
 * @param description optional description
 * @param category category
 * @param quantityRequired required quantity
 * @param unit unit label
 * @param priority priority
 */
public record CreateNeedRequest(
		@NotBlank @Size(max = 200) String title,
		String description,
		@NotNull NeedCategory category,
		@NotNull @DecimalMin(value = "0.0001") BigDecimal quantityRequired,
		@NotBlank @Size(max = 64) String unit,
		@NotNull NeedPriority priority
) {
}
