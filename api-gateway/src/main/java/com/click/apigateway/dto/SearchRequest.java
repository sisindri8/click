package com.click.apigateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What the end user (or frontend) actually sends to Click's public API:
 * just the raw search query. Everything downstream (query expansion,
 * channel search, filtering) happens behind this single call.
 */
public record SearchRequest(

        @NotBlank(message = "query must not be blank")
        @Size(max = 200, message = "query must not exceed 200 characters")
        String query

) {
}
