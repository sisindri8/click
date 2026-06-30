package com.click.apigateway.dto.youtubesearch;

import java.time.Instant;
import java.util.List;

public record VideoSearchResponse(
        int totalUniqueVideosFound,
        List<VideoResult> videos
) {
    public record VideoResult(
            String videoId,
            String title,
            String channelName,
            String channelId,
            int durationSeconds,
            Instant publishedAt,
            String thumbnailUrl,
            String url,
            int rankScore
    ) {
    }
}
