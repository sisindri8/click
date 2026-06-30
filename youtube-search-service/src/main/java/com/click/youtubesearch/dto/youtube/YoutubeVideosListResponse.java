package com.click.youtubesearch.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Maps the response shape of GET /youtube/v3/videos?part=contentDetails.
 * search.list does NOT return video duration, so we need this second call
 * to filter by our 8-20 minute window.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeVideosListResponse(
        List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String id,
            ContentDetails contentDetails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentDetails(
            String duration   // ISO-8601 format, e.g. "PT12M34S"
    ) {
    }
}
