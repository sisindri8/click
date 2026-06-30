package com.click.queryexpansion.dto;

import java.util.List;

/**
 * Response sent back to the caller (API Gateway / YouTube Search Service).
 * Contains the original query plus up to 5 semantic variations.
 */
public record QueryExpansionResponse(
        String originalQuery,
        List<String> expandedQueries,
        String detectedCategory   // e.g. "mobiles", "laptops", "audio" - used downstream for channel filtering
) {
}
