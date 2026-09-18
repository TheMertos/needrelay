package com.yagci.needrelay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the Vite SPA from classpath:/static with client-route fallback to index.html.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

	/**
	 * Registers static handlers and SPA fallback (skips API/actuator/docs paths).
	 *
	 * @param registry resource handler registry
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
				.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						if (isBackendPath(resourcePath)) {
							return null;
						}
						Resource requested = location.createRelative(resourcePath);
						if (requested.exists() && requested.isReadable()) {
							return requested;
						}
						return new ClassPathResource("/static/index.html");
					}
				});
	}

	/**
	 * Returns whether the path belongs to API or management surfaces (no SPA fallback).
	 *
	 * @param resourcePath request path relative to context
	 * @return true when Spring MVC controllers should handle the request
	 */
	private static boolean isBackendPath(String resourcePath) {
		return resourcePath.startsWith("api/")
				|| resourcePath.startsWith("actuator/")
				|| resourcePath.startsWith("v3/")
				|| resourcePath.startsWith("swagger-ui")
				|| resourcePath.equals("swagger-ui.html");
	}
}
