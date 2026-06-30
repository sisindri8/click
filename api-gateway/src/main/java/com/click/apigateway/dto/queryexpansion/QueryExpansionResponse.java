package com.click.apigateway.dto.queryexpansion;

import java.util.List;

public record QueryExpansionResponse(
        String originalQuery,
        List<String> expandedQueries,
        String detectedCategory
) {
}
