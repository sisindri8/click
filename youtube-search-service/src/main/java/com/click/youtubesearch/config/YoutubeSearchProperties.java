package com.click.youtubesearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the youtube.search.* section of application.yml into a typed object,
 * including our hardcoded list of trusted channels. Keeping this in config
 * (not in code) means adding a 6th channel later is a one-line YAML change,
 * not a code change.
 */
@ConfigurationProperties(prefix = "youtube.search")
public record YoutubeSearchProperties(
        List<TrustedChannel> trustedChannels,
        int minDurationSeconds,
        int maxDurationSeconds,
        int maxAgeMonths,
        int maxResultsPerQuery
) {
    public record TrustedChannel(String id, String name) {
    }
}
