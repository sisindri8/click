package com.click.apigateway.dto.curation;

import java.util.List;

public record CurationRequest(
        String originalQuery,
        String detectedCategory,
        List<VideoSummary> summarizations
) {
    public record VideoSummary(
            String videoId,
            String channelName,
            List<ExtractedProduct> products
    ) {}

    public record ExtractedProduct(
            String name,
            String price,
            List<String> pros,
            List<String> cons,
            boolean recommended,
            String recommendationReason
    ) {}
}
