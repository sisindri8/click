package com.click.youtubesearch.util;

import com.click.youtubesearch.dto.VideoSearchResponse;

/**
 * Combines the three independent signals into one final ranking score:
 *
 *   relevanceScore  (keyword overlap with original query, see RelevanceFilter)
 *   categoryScore   (does the title mention the detected category, see CategoryFilter)
 *   freshnessScore  (how recent the video is, see FreshnessScorer)
 *
 * Weights are deliberately simple and tunable in one place. Relevance is
 * weighted heaviest since it's the most direct signal of "did this video
 * actually answer the query" - category and freshness are tie-breakers /
 * secondary boosts, not primary filters.
 */
public final class VideoRanker {

    private VideoRanker() {
    }

    private static final int RELEVANCE_WEIGHT = 10;
    private static final int CATEGORY_MATCH_BONUS = 15;

    public static int rankScore(String originalQuery, String category, VideoSearchResponse.VideoResult video) {
        int relevanceScore = RelevanceFilter.relevanceScore(originalQuery, video.title()) * RELEVANCE_WEIGHT;

        int categorySignal = CategoryFilter.score(category, video.title());
        int categoryScore = categorySignal == 1 ? CATEGORY_MATCH_BONUS : 0; // unrecognized category (-1) or no match (0) both contribute nothing

        int freshnessScore = FreshnessScorer.score(video.publishedAt());

        return relevanceScore + categoryScore + freshnessScore;
    }
}
