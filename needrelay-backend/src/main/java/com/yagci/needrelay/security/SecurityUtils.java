package com.yagci.needrelay.security;

import com.yagci.needrelay.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helpers for reading the current authenticated organizer.
 */
public final class SecurityUtils {

	private SecurityUtils() {
	}

	/**
	 * Returns the current {@link OrganizerPrincipal} from the security context.
	 *
	 * @return authenticated principal
	 * @throws ApiException if no organizer is authenticated
	 */
	public static OrganizerPrincipal getCurrentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof OrganizerPrincipal principal)) {
			throw new ApiException("UNAUTHORIZED", "Authentication required", HttpStatus.UNAUTHORIZED);
		}
		return principal;
	}
}
