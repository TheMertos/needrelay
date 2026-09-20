package com.yagci.needrelay.security;

import com.yagci.needrelay.domain.OrganizationRole;
import com.yagci.needrelay.domain.OrganizerRole;
import lombok.AllArgsConstructor;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads Bearer JWT and populates the security context.
 */
@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			try {
				Claims claims = jwtService.parse(token);
				UUID id = UUID.fromString(claims.getSubject());
				String email = claims.get("email", String.class);
				OrganizerRole role = OrganizerRole.valueOf(claims.get("role", String.class));
				String organizationIdClaim = claims.get("organizationId", String.class);
				String organizationRoleClaim = claims.get("organizationRole", String.class);
				UUID organizationId = organizationIdClaim != null ? UUID.fromString(organizationIdClaim) : null;
				OrganizationRole organizationRole =
						organizationRoleClaim != null ? OrganizationRole.valueOf(organizationRoleClaim) : null;
				OrganizerPrincipal principal =
						new OrganizerPrincipal(id, email, "", role, true, organizationId, organizationRole);
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			catch (Exception ignored) {
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}
}
