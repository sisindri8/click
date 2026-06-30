package com.click.apigateway.controller;

import com.click.apigateway.dto.SearchRequest;
import com.click.apigateway.dto.SearchResponse;
import com.click.apigateway.service.SearchOrchestrationService;
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
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchOrchestrationService searchOrchestrationService;

    /**
     * POST /api/v1/search
     * Body: { "query": "best phones under 30k" }
     * <p>
     * This is the single public entry point for Click. Internally it chains:
     * Query Expansion Service -> YouTube Search Service, and returns the
     * combined result. This is the real pipeline test - if this works,
     * the two services are genuinely talking to each other over HTTP.
     */
    @PostMapping
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        log.info("Received search request: '{}'", request.query());
        SearchResponse response = searchOrchestrationService.search(request.query());
        log.info("Search complete: {} videos found for category '{}'",
                response.totalUniqueVideosFound(), response.detectedCategory());
        return ResponseEntity.ok(response);
    }
}
