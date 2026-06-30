package com.click.youtubesearch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class YoutubeApiConfig {

    @Value("${youtube.api.base-url}")
    private String baseUrl;

    @Bean
    public WebClient youtubeWebClient() {
        // API key is sent as a query param per request (YouTube Data API convention),
        // not as a header - see YoutubeApiClient for usage.
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
