package com.click.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record ServiceUrlsProperties(
        QueryExpansion queryExpansion,
        YoutubeSearch youtubeSearch,
        Transcript transcript,
        Summarization summarization,
        Curation curation
) {
    public record QueryExpansion(String baseUrl) {}
    public record YoutubeSearch(String baseUrl) {}
    public record Transcript(String baseUrl) {}
    public record Summarization(String baseUrl) {}
    public record Curation(String baseUrl) {}
}
