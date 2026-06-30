package com.click.curation.controller;

import com.click.curation.dto.CurationRequest;
import com.click.curation.dto.CurationResponse;
import com.click.curation.service.CurationService;
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
@RequestMapping("/api/v1/curation")
@RequiredArgsConstructor
public class CurationController {

    private final CurationService curationService;

    /**
     * POST /api/v1/curation/curate
     * Body: { "originalQuery": "...", "detectedCategory": "...", "summarizations": [...] }
     * <p>
     * Takes all extracted product data from multiple video summaries and
     * returns max 5 curated product cards grouped by use case.
     * This is the final step — what the user actually sees.
     */
    @PostMapping("/curate")
    public ResponseEntity<CurationResponse> curate(@Valid @RequestBody CurationRequest request) {
        log.info("Received curation request for query '{}' with {} video summaries",
                request.originalQuery(), request.summarizations().size());
        CurationResponse response = curationService.curate(request);
        log.info("Curation complete: {} cards produced", response.curatedPicks().size());
        return ResponseEntity.ok(response);
    }
}
