package com.click.youtubesearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Incoming request from API Gateway / orchestrator.
 *
 * @param originalQuery the user's raw, un-expanded search query - used for
 *                       relevance scoring against video titles
 * @param queries        the 5 expanded query variations from Query Expansion
 *                       Service, used to actually broaden the YouTube search
 * @param category       detected product category from Query Expansion Service
 *                       (e.g. "mobiles", "laptops") - used as a secondary
 *                       ranking signal. Optional - pass null/blank if unknown.
 */
public record VideoSearchRequest(

        @NotBlank(message = "originalQuery must not be blank")
        @Size(max = 200)
        String originalQuery,

        @NotEmpty(message = "queries must not be empty")
        @Size(max = 10, message = "no more than 10 query variations allowed per search")
        List<String> queries,

        String category

) {
}
