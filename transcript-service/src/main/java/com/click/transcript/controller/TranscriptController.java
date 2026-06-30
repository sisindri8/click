package com.click.transcript.controller;

import com.click.transcript.dto.TranscriptRequest;
import com.click.transcript.dto.TranscriptResponse;
import com.click.transcript.service.TranscriptService;
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
@RequestMapping("/api/v1/transcript")
@RequiredArgsConstructor
public class TranscriptController {

    private final TranscriptService transcriptService;

    /**
     * POST /api/v1/transcript/fetch
     * Body: { "videoId": "dQw4w9WgXcQ" }
     * <p>
     * Downloads audio and transcribes via Whisper (or returns mock data if
     * transcript.mock-mode=true). Can take 10-90 seconds for a real call -
     * this is the slowest step in the whole pipeline, by design Whisper
     * trades speed for not depending on YouTube captions being scrapeable.
     */
    @PostMapping("/fetch")
    public ResponseEntity<TranscriptResponse> fetch(@Valid @RequestBody TranscriptRequest request) {
        log.info("Received transcript request for video '{}'", request.videoId());
        TranscriptResponse response = transcriptService.fetchTranscript(request.videoId());
        log.info("Transcript fetched for '{}': {} words, source={}",
                request.videoId(), response.wordCount(), response.source());
        return ResponseEntity.ok(response);
    }
}
