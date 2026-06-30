package com.click.queryexpansion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming request: the raw search query typed by the user.
 * e.g. { "query": "best phones under 30k" }
 */
public record QueryExpansionRequest(

        @NotBlank(message = "query must not be blank")
        @Size(max = 200, message = "query must not exceed 200 characters")
        String query

) {
}
