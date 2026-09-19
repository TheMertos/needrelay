package com.yagci.needrelay.security;

import com.yagci.needrelay.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

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

	/**
	 * Returns the current principal's organization id, or 403 if they have none
	 * (platform admins are not members of any organization).
	 *
	 * @param principal current principal
	 * @return organization id
	 * @throws ApiException if the principal has no organization
	 */
	public static UUID requireOrganizationId(OrganizerPrincipal principal) {
		if (principal.getOrganizationId() == null) {
			throw new ApiException("NO_ORGANIZATION", "This account has no organization", HttpStatus.FORBIDDEN);
		}
		return principal.getOrganizationId();
	}
}
