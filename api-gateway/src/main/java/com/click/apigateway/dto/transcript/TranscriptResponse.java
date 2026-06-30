package com.click.apigateway.dto.transcript;

public record TranscriptResponse(
        String videoId,
        String transcript,
        String source,
        int wordCount
) {
}
