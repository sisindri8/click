package com.click.summarization.controller;

import com.click.summarization.dto.SummarizationRequest;
import com.click.summarization.dto.SummarizationResponse;
import com.click.summarization.service.SummarizationService;
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
@RequestMapping("/api/v1/summarization")
@RequiredArgsConstructor
public class SummarizationController {

    private final SummarizationService summarizationService;

    /**
     * POST /api/v1/summarization/summarize
     * Body: { "videoId": "...", "transcript": "...", "channelName": "..." }
     * <p>
     * Sends the transcript to Claude and returns structured product data:
     * name, price, pros, cons, and reviewer recommendation per product.
     */
    @PostMapping("/summarize")
    public ResponseEntity<SummarizationResponse> summarize(@Valid @RequestBody SummarizationRequest request) {
        log.info("Received summarization request for video '{}'", request.videoId());
        SummarizationResponse response = summarizationService.summarize(request);
        log.info("Summarization complete for '{}': {} products extracted, source={}",
                request.videoId(), response.products().size(), response.source());
        return ResponseEntity.ok(response);
    }
}
