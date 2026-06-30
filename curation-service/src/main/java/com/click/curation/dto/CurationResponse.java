package com.click.curation.dto;

import java.util.List;

/**
 * The final output of the entire Click pipeline — what the user actually sees.
 * Max 5 curated product cards, each representing the best product in one category.
 */
public record CurationResponse(
        String originalQuery,
        String detectedCategory,
        int totalVideosAnalyzed,
        List<CuratedCard> curatedPicks,
        String source   // "claude" or "mock"
) {

    public record CuratedCard(
            String category,        // e.g. "Best Overall", "Best Camera Phone"
            String emoji,           // e.g. "🏆", "📸", "🔋", "⚡", "💰"
            String productName,
            String price,
            String confidence,      // "HIGH", "MEDIUM", "LOW"
            List<String> keyReasons,        // top 3 reasons why this won its category
            String verdict,                 // one-line summary
            String channelName,             // which channel recommended it
            String videoUrl,                // link to the source review
            String buyUrl                   // Amazon affiliate search link - see AffiliateLinkBuilder
    ) {
    }
}
