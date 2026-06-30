package com.click.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({ServiceUrlsProperties.class, PipelineProperties.class})
@RequiredArgsConstructor
public class DownstreamClientConfig {

    private final ServiceUrlsProperties serviceUrls;

    @Bean
    public WebClient queryExpansionWebClient() {
        return WebClient.builder()
                .baseUrl(serviceUrls.queryExpansion().baseUrl())
                .build();
    }

    @Bean
    public WebClient youtubeSearchWebClient() {
        return WebClient.builder()
                .baseUrl(serviceUrls.youtubeSearch().baseUrl())
                .build();
    }

    @Bean
    public WebClient transcriptWebClient() {
        return WebClient.builder()
                .baseUrl(serviceUrls.transcript().baseUrl())
                .build();
    }

    @Bean
    public WebClient summarizationWebClient() {
        return WebClient.builder()
                .baseUrl(serviceUrls.summarization().baseUrl())
                .build();
    }

    @Bean
    public WebClient curationWebClient() {
        return WebClient.builder()
                .baseUrl(serviceUrls.curation().baseUrl())
                .build();
    }
}
