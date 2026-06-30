package com.click.youtubesearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * YouTube Search Service
 * <p>
 * Takes the expanded queries from Query Expansion Service and searches
 * YouTube Data API v3, scoped ONLY to our trusted channel whitelist.
 * Filters by video duration and recency, deduplicates results across
 * the 5 query variations, and returns the top relevant videos.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class YoutubeSearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoutubeSearchServiceApplication.class, args);
    }
}
