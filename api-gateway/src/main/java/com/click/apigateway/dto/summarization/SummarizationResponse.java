package com.click.apigateway.dto.summarization;

import java.util.List;

public record SummarizationResponse(
        String videoId,
        String channelName,
        List<ExtractedProduct> products,
        String source
) {
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
