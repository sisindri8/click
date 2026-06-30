package com.click.queryexpansion.controller;

import com.click.queryexpansion.dto.QueryExpansionRequest;
import com.click.queryexpansion.dto.QueryExpansionResponse;
import com.click.queryexpansion.service.ClaudeQueryExpansionService;
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
@RequestMapping("/api/v1/query-expansion")
@RequiredArgsConstructor
public class QueryExpansionController {

    private final ClaudeQueryExpansionService claudeQueryExpansionService;

    /**
     * POST /api/v1/query-expansion/expand
     * Body: { "query": "best phones under 30k" }
     * <p>
     * Returns 5 semantic variations of the query plus a detected product category.
     */
    @PostMapping("/expand")
    public ResponseEntity<QueryExpansionResponse> expandQuery(@Valid @RequestBody QueryExpansionRequest request) {
        log.info("Received query expansion request: '{}'", request.query());
        QueryExpansionResponse response = claudeQueryExpansionService.expand(request.query());
        log.info("Expanded '{}' into {} variations, category='{}'",
                request.query(), response.expandedQueries().size(), response.detectedCategory());
        return ResponseEntity.ok(response);
    }
}
