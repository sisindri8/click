package com.click.transcript.dto;

public record TranscriptResponse(
        String videoId,
        String transcript,
        String source,       // "whisper" or "mock"
        int wordCount
) {
}
