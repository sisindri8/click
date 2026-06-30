package com.click.youtubesearch.service;

import com.click.youtubesearch.dto.youtube.YoutubeSearchListResponse;
import com.click.youtubesearch.dto.youtube.YoutubeVideosListResponse;
import com.click.youtubesearch.exception.YoutubeApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Thin wrapper around the two YouTube Data API v3 endpoints we need:
 * - search.list: find videos matching a query, scoped to one channel
 * - videos.list: fetch duration for a batch of video IDs (search.list
 *   doesn't include duration, so this is a required second call)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeApiClient {

    private final WebClient youtubeWebClient;

    @Value("${youtube.api.key}")
    private String apiKey;

    public YoutubeSearchListResponse searchChannel(String query, String channelId, int maxAgeMonths, int maxResults) {
        String publishedAfter = Instant.now().minus(maxAgeMonths * 30L, ChronoUnit.DAYS).toString();

        try {
            return youtubeWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("channelId", channelId)
                            .queryParam("type", "video")
                            .queryParam("order", "relevance")
                            .queryParam("publishedAfter", publishedAfter)
                            .queryParam("maxResults", maxResults)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(YoutubeSearchListResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("YouTube search.list failed for query='{}', channelId='{}': {}", query, channelId, e.getMessage());
            throw new YoutubeApiException("YouTube search failed for channel " + channelId, e);
        }
    }

    /**
     * YouTube's videos.list endpoint accepts a MAXIMUM of 50 IDs per request -
     * exceeding this returns a 400 Bad Request for the entire call, not just
     * the excess IDs (confirmed during testing: 51 IDs failed outright).
     * With 5 query variations x 5 channels, it's common to collect 50+ unique
     * videos, so this batches into chunks of 50 and merges the results.
     */
    private static final int MAX_IDS_PER_REQUEST = 50;

    public YoutubeVideosListResponse fetchVideoDurations(List<String> videoIds) {
        if (videoIds.isEmpty()) {
            return new YoutubeVideosListResponse(List.of());
        }

        List<YoutubeVideosListResponse.Item> allItems = new java.util.ArrayList<>();

        for (int i = 0; i < videoIds.size(); i += MAX_IDS_PER_REQUEST) {
            List<String> batch = videoIds.subList(i, Math.min(i + MAX_IDS_PER_REQUEST, videoIds.size()));
            YoutubeVideosListResponse batchResponse = fetchVideoDurationsBatch(batch);
            if (batchResponse != null && batchResponse.items() != null) {
                allItems.addAll(batchResponse.items());
            }
        }

        return new YoutubeVideosListResponse(allItems);
    }

    private YoutubeVideosListResponse fetchVideoDurationsBatch(List<String> videoIds) {
        try {
            return youtubeWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/videos")
                            .queryParam("part", "contentDetails")
                            .queryParam("id", String.join(",", videoIds))
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(YoutubeVideosListResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("YouTube videos.list batch failed for {} video IDs: {}", videoIds.size(), e.getMessage());
            throw new YoutubeApiException("YouTube video details fetch failed", e);
        }
    }
}
