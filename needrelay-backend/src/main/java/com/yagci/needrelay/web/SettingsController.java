package com.yagci.needrelay.web;

import com.yagci.needrelay.service.SystemSettingsService;
import com.yagci.needrelay.web.dto.SystemSettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated platform settings (currently just the default UI language).
 */
@RestController
@RequestMapping("/api/settings")
@Tag(name = "settings")
public class SettingsController {

	private final SystemSettingsService systemSettingsService;

	/**
	 * @param systemSettingsService settings service
	 */
	public SettingsController(SystemSettingsService systemSettingsService) {
		this.systemSettingsService = systemSettingsService;
	}

	/**
	 * Returns the platform's default UI language.
	 *
	 * @return settings
	 */
	@GetMapping
	@Operation(summary = "Get public platform settings")
	@ApiResponse(responseCode = "200", description = "Settings")
	public SystemSettingsResponse get() {
		return systemSettingsService.get();
	}
}
