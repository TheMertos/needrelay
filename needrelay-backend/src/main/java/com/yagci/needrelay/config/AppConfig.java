package com.yagci.needrelay.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables typed NeedRelay configuration properties.
 */
@Configuration
@EnableConfigurationProperties(NeedRelayProperties.class)
public class AppConfig {
}
