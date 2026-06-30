package com.click.queryexpansion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures a single reusable WebClient bean for calling the Anthropic API.
 * Base URL and API key come from application.yml (which reads from env vars -
 * never hardcode the key here).
 */
@Configuration
public class ClaudeApiConfig {

    @Value("${claude.api.base-url}")
    private String baseUrl;

    @Value("${claude.api.key}")
    private String apiKey;

    @Bean
    public WebClient claudeWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }
}
