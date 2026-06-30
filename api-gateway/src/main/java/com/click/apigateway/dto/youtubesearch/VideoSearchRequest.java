package com.click.apigateway.dto.youtubesearch;

import java.util.List;

public record VideoSearchRequest(
        String originalQuery,
        List<String> queries,
        String category
) {
}
