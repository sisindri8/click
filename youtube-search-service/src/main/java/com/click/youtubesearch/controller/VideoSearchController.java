package com.click.youtubesearch.controller;

import com.click.youtubesearch.dto.VideoSearchRequest;
import com.click.youtubesearch.dto.VideoSearchResponse;
import com.click.youtubesearch.service.VideoSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/youtube-search")
@RequiredArgsConstructor
public class VideoSearchController {

    private final VideoSearchService videoSearchService;

    /**
     * POST /api/v1/youtube-search/search
     * Body: { "originalQuery": "best phones under 30k", "queries": [...], "category": "mobiles" }
     * <p>
     * Searches all 5 trusted channels for each expanded query, deduplicates,
     * filters by duration (8-20 min) and recency (last 12 months), drops
     * videos with no keyword overlap with the original query, then ranks
     * the rest by a combined relevance + category + freshness score.
     */
    @PostMapping("/search")
    public ResponseEntity<VideoSearchResponse> search(@Valid @RequestBody VideoSearchRequest request) {
        log.info("Searching {} query variations across trusted channels for original query '{}' (category={})",
                request.queries().size(), request.originalQuery(), request.category());
        VideoSearchResponse response = videoSearchService.search(
                request.originalQuery(), request.queries(), request.category());
        return ResponseEntity.ok(response);
    }
}
