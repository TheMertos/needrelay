package com.yagci.needrelay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI metadata for NeedRelay.
 */
@Configuration
public class OpenApiConfig {

	/**
	 * Builds the OpenAPI document with JWT bearer security.
	 *
	 * @return OpenAPI bean titled NeedRelay API
	 */
	@Bean
	public OpenAPI needRelayOpenApi() {
		final String schemeName = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("NeedRelay API")
						.version("1.0")
						.description("Crisis resource coordination API"))
				.addSecurityItem(new SecurityRequirement().addList(schemeName))
				.components(new Components()
						.addSecuritySchemes(schemeName, new SecurityScheme()
								.name(schemeName)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
