package com.click.summarization.dto;

import java.util.List;

/**
 * Final response: one video's worth of extracted product data.
 * Each product in the list was mentioned in the transcript with enough
 * detail to extract at least a name and one other attribute.
 */
public record SummarizationResponse(
        String videoId,
        String channelName,
        List<ExtractedProduct> products,
        String source   // "claude" or "mock"
) {

    /**
     * One product extracted from the transcript.
     * All fields except name are nullable - a reviewer might mention a phone
     * without stating its price, or praise it without listing specific pros.
     * Nulls here are expected and handled downstream in Curation Service.
     */
    public record ExtractedProduct(
            String name,
            String price,           // raw string e.g. "₹27,999" or "27k" - normalized later
            List<String> pros,      // max 4 points - we cap this in the prompt
            List<String> cons,      // max 2 points
            boolean recommended,    // did the reviewer recommend this product?
            String recommendationReason  // one-line reason if recommended
    ) {
    }
}
