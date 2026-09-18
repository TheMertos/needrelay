package com.yagci.needrelay.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves the client IP from proxy headers or the remote address.
 */
@Component
public class ClientIpResolver {

	/**
	 * Returns the best-effort client IP for rate limiting.
	 *
	 * @param request HTTP request
	 * @return IP address string
	 */
	public String resolve(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(forwarded)) {
			String first = forwarded.split(",")[0].trim();
			if (StringUtils.hasText(first)) {
				return first;
			}
		}
		String realIp = request.getHeader("X-Real-IP");
		if (StringUtils.hasText(realIp)) {
			return realIp.trim();
		}
		String remote = request.getRemoteAddr();
		return remote != null ? remote : "unknown";
	}
}
