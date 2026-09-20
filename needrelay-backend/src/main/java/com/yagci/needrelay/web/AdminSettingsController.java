package com.yagci.needrelay.web;

import com.yagci.needrelay.service.SystemSettingsService;
import com.yagci.needrelay.web.dto.SystemSettingsResponse;
import com.yagci.needrelay.web.dto.UpdateSystemSettingsRequest;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only platform settings management.
 */
@RestController
@RequestMapping("/api/admin/settings")
@Tag(name = "admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
public class AdminSettingsController {

	private final SystemSettingsService systemSettingsService;

	/**
	 * Updates the platform's default UI language.
	 *
	 * @param request new default language
	 * @return updated settings
	 */
	@PutMapping
	@Operation(summary = "Update platform settings")
	@ApiResponse(responseCode = "200", description = "Updated")
	public SystemSettingsResponse update(@Valid @RequestBody UpdateSystemSettingsRequest request) {
		return systemSettingsService.update(request);
	}
}
