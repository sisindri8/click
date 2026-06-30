package com.click.youtubesearch.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lightweight keyword-overlap relevance filter.
 * <p>
 * YouTube's own search.list relevance ranking is loose when scoped to a
 * single channel - it can surface a channel's generally popular videos even
 * when they don't match the query topic. This filter is a cheap, no-extra-API-call
 * sanity check: does the video title actually share meaningful words with
 * what the user searched for?
 * <p>
 * This is NOT semantic matching (that's what the future Summarization step
 * does with Claude) - it's a fast pre-filter to cut obviously unrelated
 * results before we spend any AI tokens on them.
 */
public final class RelevanceFilter {

    private RelevanceFilter() {
    }

    // Common words that appear in almost every query but carry no topic signal -
    // matching on these alone would let anything through.
    private static final Set<String> STOPWORDS = Set.of(
            "best", "top", "under", "review", "2024", "2025", "2026",
            "the", "a", "an", "for", "in", "of", "to", "vs", "and", "or",
            "price", "india", "rupees", "rs", "budget", "comparison"
    );

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]+");

    /**
     * Returns true if the video title shares at least one meaningful
     * (non-stopword) keyword with the query.
     */
    public static boolean isRelevant(String query, String videoTitle) {
        if (query == null || videoTitle == null) {
            return false;
        }

        Set<String> queryKeywords = extractKeywords(query);
        Set<String> titleKeywords = extractKeywords(videoTitle);

        if (queryKeywords.isEmpty()) {
            // Query was entirely stopwords/numbers - can't filter meaningfully, let it through
            return true;
        }

        return queryKeywords.stream().anyMatch(titleKeywords::contains);
    }

    /**
     * Scores 0-N based on how many meaningful keywords overlap - useful later
     * for ranking rather than just a hard yes/no filter.
     */
    public static int relevanceScore(String query, String videoTitle) {
        if (query == null || videoTitle == null) {
            return 0;
        }
        Set<String> queryKeywords = extractKeywords(query);
        Set<String> titleKeywords = extractKeywords(videoTitle);

        return (int) queryKeywords.stream().filter(titleKeywords::contains).count();
    }

    private static Set<String> extractKeywords(String text) {
        Set<String> keywords = new LinkedHashSet<>();
        String[] tokens = NON_ALPHANUMERIC.split(text.toLowerCase());

        for (String token : tokens) {
            if (token.isBlank() || token.length() < 3) {
                continue; // drop very short tokens (e.g. "a", "5g") - too noisy to be a reliable topic signal
            }
            if (STOPWORDS.contains(token)) {
                continue;
            }
            keywords.add(token);
        }
        return keywords;
    }
}
