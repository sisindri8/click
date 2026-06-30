package com.click.apigateway.dto.curation;

import java.util.List;

public record CurationResponse(
        String originalQuery,
        String detectedCategory,
        int totalVideosAnalyzed,
        List<CuratedCard> curatedPicks,
        String source
) {
    public record CuratedCard(
            String category,
            String emoji,
            String productName,
            String price,
            String confidence,
            List<String> keyReasons,
            String verdict,
            String channelName,
            String videoUrl,
            String buyUrl
    ) {}
}
