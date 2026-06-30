package com.click.youtubesearch.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps the response shape of GET /youtube/v3/search.
 * Only fields we actually use are mapped - everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeSearchListResponse(
        List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Id id,
            Snippet snippet
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Id(
            @JsonProperty("videoId") String videoId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippet(
            String title,
            String channelId,
            String channelTitle,
            String publishedAt,
            Thumbnails thumbnails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnails(
            Thumbnail high,
            Thumbnail medium,
            @JsonProperty("default") Thumbnail defaultThumbnail
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnail(String url) {
    }
}
