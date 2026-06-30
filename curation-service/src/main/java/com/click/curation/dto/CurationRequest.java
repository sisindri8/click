package com.click.curation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CurationRequest(

        @NotBlank(message = "originalQuery must not be blank")
        String originalQuery,

        String detectedCategory,   // e.g. "mobiles" - helps Claude pick relevant categories

        @NotEmpty(message = "summarizations must not be empty")
        List<VideoSummary> summarizations

) {
    /**
     * One video's worth of extracted products - matches what
     * Summarization Service returns per video.
     */
    public record VideoSummary(
            String videoId,
            String channelName,
            List<ExtractedProduct> products
    ) {
    }

    public record ExtractedProduct(
            String name,
            String price,
            List<String> pros,
            List<String> cons,
            boolean recommended,
            String recommendationReason
    ) {
    }
}
