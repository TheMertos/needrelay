package com.yagci.needrelay.web;

import com.yagci.needrelay.exception.ApiException;
import com.yagci.needrelay.security.ClientIpResolver;
import com.yagci.needrelay.security.LoginRateLimiter;
import com.yagci.needrelay.security.OrganizerPrincipal;
import com.yagci.needrelay.security.SecurityUtils;
import com.yagci.needrelay.service.AuthService;
import com.yagci.needrelay.web.dto.ChangePasswordRequest;
import com.yagci.needrelay.web.dto.ForgotPasswordRequest;
import com.yagci.needrelay.web.dto.LoginRequest;
import com.yagci.needrelay.web.dto.LogoutRequest;
import com.yagci.needrelay.web.dto.OrganizerResponse;
import com.yagci.needrelay.web.dto.RefreshRequest;
import com.yagci.needrelay.web.dto.RegisterRequest;
import com.yagci.needrelay.web.dto.ResetPasswordRequest;
import com.yagci.needrelay.web.dto.TokenResponse;
import com.yagci.needrelay.web.dto.UpdateProfileRequest;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication and account self-service endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth")
@AllArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final LoginRateLimiter loginRateLimiter;
	private final ClientIpResolver clientIpResolver;

	/**
	 * Registers a new organizer with an invite token.
	 *
	 * @param request registration payload
	 * @return tokens
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register with invite token")
	@ApiResponse(responseCode = "201", description = "Registered")
	public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	/**
	 * Logs in with email and password.
	 *
	 * @param request login payload
	 * @param httpRequest servlet request for IP rate limiting
	 * @return tokens
	 */
	@PostMapping("/login")
	@Operation(summary = "Login")
	@ApiResponse(responseCode = "200", description = "Authenticated")
	@ApiResponse(responseCode = "401", description = "Invalid credentials")
	@ApiResponse(responseCode = "429", description = "Too many login attempts")
	public TokenResponse login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		String clientIp = clientIpResolver.resolve(httpRequest);
		if (!loginRateLimiter.tryConsume(clientIp)) {
			throw new ApiException(
					"RATE_LIMITED",
					"Too many login attempts. Please try again later.",
					HttpStatus.TOO_MANY_REQUESTS);
		}
		return authService.login(request);
	}

	/**
	 * Rotates a refresh token.
	 *
	 * @param request refresh payload
	 * @return new tokens
	 */
	@PostMapping("/refresh")
	@Operation(summary = "Refresh tokens")
	@ApiResponse(responseCode = "200", description = "Tokens rotated")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request);
	}

	/**
	 * Revokes a refresh token.
	 *
	 * @param request logout payload
	 */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Logout")
	@ApiResponse(responseCode = "204", description = "Refresh token revoked")
	public void logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request);
	}

	/**
	 * Starts a password-reset email flow.
	 *
	 * @param request email payload
	 */
	@PostMapping("/forgot-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Request password reset email")
	@ApiResponse(responseCode = "204", description = "Accepted")
	public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request);
	}

	/**
	 * Completes password reset with emailed token.
	 *
	 * @param request token and new password
	 */
	@PostMapping("/reset-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Reset password with token")
	@ApiResponse(responseCode = "204", description = "Password updated")
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
	}

	/**
	 * Returns the current organizer.
	 *
	 * @return profile
	 */
	@GetMapping("/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Current organizer")
	@ApiResponse(responseCode = "200", description = "Current user")
	public OrganizerResponse me() {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return authService.me(principal.getId());
	}

	/**
	 * Updates the current organizer profile.
	 *
	 * @param request profile payload
	 * @return updated profile
	 */
	@PutMapping("/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Update profile")
	@ApiResponse(responseCode = "200", description = "Updated")
	public OrganizerResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		return authService.updateProfile(principal.getId(), request);
	}

	/**
	 * Changes the current organizer password.
	 *
	 * @param request password payload
	 */
	@PostMapping("/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Change password")
	@ApiResponse(responseCode = "204", description = "Password changed")
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		OrganizerPrincipal principal = SecurityUtils.getCurrentPrincipal();
		authService.changePassword(principal.getId(), request);
	}
}
