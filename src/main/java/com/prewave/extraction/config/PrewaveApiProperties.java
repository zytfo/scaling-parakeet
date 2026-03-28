package com.prewave.extraction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Props to access remote API
 * @param baseUrl remote API url
 * @param key API key
 */

@ConfigurationProperties(prefix = "prewave.api")
public record PrewaveApiProperties(
        String baseUrl,
        String key
) {
}
