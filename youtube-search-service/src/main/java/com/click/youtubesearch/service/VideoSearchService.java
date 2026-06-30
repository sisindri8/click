package com.click.youtubesearch.service;

import com.click.youtubesearch.config.YoutubeSearchProperties;
import com.click.youtubesearch.dto.VideoSearchResponse;
import com.click.youtubesearch.dto.youtube.YoutubeSearchListResponse;
import com.click.youtubesearch.util.DurationParser;
import com.click.youtubesearch.util.RelevanceFilter;
import com.click.youtubesearch.util.VideoRanker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates the full search flow:
 *
 *   N query variations  x  5 trusted channels
 *            |
 *            v
 *   raw YouTube search.list results (may contain duplicates -
 *   the same video can match multiple query variations)
 *            |
 *            v
 *   deduplicate by videoId
 *            |
 *            v
 *   fetch durations via videos.list, filter to 8-20 min window
 *            |
 *            v
 *   filter by recency (already partly done via publishedAfter param)
 *            |
 *            v
 *   keyword-relevance filter against the original query (drops videos whose
 *   title shares no meaningful keyword with what the user actually typed -
 *   YouTube's own channel-scoped relevance ranking is too loose on its own)
 *            |
 *            v
 *   combined ranking score (relevance + category match + freshness) -
 *   see VideoRanker for the weighting logic
 *            |
 *            v
 *   return top N unique, relevant videos, highest ranked first
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoSearchService {

    private final YoutubeApiClient youtubeApiClient;
    private final YoutubeSearchProperties properties;

    /**
     * @param originalQuery the user's original, un-expanded query (e.g. "best phones under 30k") -
     *                      used for relevance scoring since it's the clearest signal of true intent
     * @param queries       the expanded query variations to actually search YouTube with
     * @param category      detected product category (e.g. "mobiles") used as a secondary ranking
     *                      signal - pass null if unknown, ranking just skips that signal
     */
    public VideoSearchResponse search(String originalQuery, List<String> queries, String category) {
        // Step 1: search every query against every trusted channel, collect raw results
        Map<String, YoutubeSearchListResponse.Item> uniqueVideosById = new LinkedHashMap<>();

        for (String query : queries) {
            for (YoutubeSearchProperties.TrustedChannel channel : properties.trustedChannels()) {
                YoutubeSearchListResponse result = youtubeApiClient.searchChannel(
                        query, channel.id(), properties.maxAgeMonths(), properties.maxResultsPerQuery());

                if (result == null || result.items() == null) {
                    continue;
                }

                for (YoutubeSearchListResponse.Item item : result.items()) {
                    String videoId = item.id().videoId();
                    // LinkedHashMap.putIfAbsent naturally deduplicates - if the same
                    // video matched 3 different query variations, we only keep it once.
                    uniqueVideosById.putIfAbsent(videoId, item);
                }
            }
        }

        log.info("Found {} unique videos across {} queries x {} channels",
                uniqueVideosById.size(), queries.size(), properties.trustedChannels().size());

        if (uniqueVideosById.isEmpty()) {
            return new VideoSearchResponse(0, List.of());
        }

        // Step 2: fetch durations for all unique videos in one batched call
        List<String> videoIds = uniqueVideosById.keySet().stream().toList();
        var durationsResponse = youtubeApiClient.fetchVideoDurations(videoIds);

        Map<String, Integer> durationByVideoId = durationsResponse.items().stream()
                .collect(Collectors.toMap(
                        item -> item.id(),
                        item -> DurationParser.toSeconds(item.contentDetails().duration())
                ));

        // Step 3: filter by duration window (8-20 min reviews)
        List<VideoSearchResponse.VideoResult> durationFiltered = uniqueVideosById.entrySet().stream()
                .map(entry -> toVideoResult(entry.getKey(), entry.getValue(),
                        durationByVideoId.getOrDefault(entry.getKey(), 0)))
                .filter(v -> v.durationSeconds() >= properties.minDurationSeconds()
                        && v.durationSeconds() <= properties.maxDurationSeconds())
                .toList();

        log.info("{} videos remain after duration filter ({}s - {}s)",
                durationFiltered.size(), properties.minDurationSeconds(), properties.maxDurationSeconds());

        // Step 4: keyword-relevance filter against the ORIGINAL query (hard filter - drops
        // videos with zero keyword overlap, since those are almost certainly noise)
        List<VideoSearchResponse.VideoResult> relevantVideos = durationFiltered.stream()
                .filter(v -> RelevanceFilter.isRelevant(originalQuery, v.title()))
                .toList();

        log.info("{} videos remain after relevance filter (dropped {} irrelevant titles)",
                relevantVideos.size(), durationFiltered.size() - relevantVideos.size());

        // Step 5: combined ranking - relevance + category match + freshness, highest score first.
        // Score is attached to each video (not just used for sorting) so it's visible in the
        // API response - useful for debugging why one video ranked above another.
        List<VideoSearchResponse.VideoResult> rankedVideos = relevantVideos.stream()
                .map(v -> withRankScore(v, VideoRanker.rankScore(originalQuery, category, v)))
                .sorted(Comparator.comparingInt(VideoSearchResponse.VideoResult::rankScore).reversed())
                .toList();

        return new VideoSearchResponse(rankedVideos.size(), rankedVideos);
    }

    private VideoSearchResponse.VideoResult withRankScore(VideoSearchResponse.VideoResult video, int rankScore) {
        return new VideoSearchResponse.VideoResult(
                video.videoId(), video.title(), video.channelName(), video.channelId(),
                video.durationSeconds(), video.publishedAt(), video.thumbnailUrl(), video.url(),
                rankScore
        );
    }

    private VideoSearchResponse.VideoResult toVideoResult(String videoId,
                                                            YoutubeSearchListResponse.Item item,
                                                            int durationSeconds) {
        var snippet = item.snippet();
        String thumbnailUrl = snippet.thumbnails() != null && snippet.thumbnails().high() != null
                ? snippet.thumbnails().high().url()
                : null;

        return new VideoSearchResponse.VideoResult(
                videoId,
                snippet.title(),
                snippet.channelTitle(),
                snippet.channelId(),
                durationSeconds,
                Instant.parse(snippet.publishedAt()),
                thumbnailUrl,
                "https://www.youtube.com/watch?v=" + videoId,
                0 // rankScore computed later in Step 5, placeholder here
        );
    }
}
