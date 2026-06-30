package com.click.apigateway.dto;

import com.click.apigateway.dto.curation.CurationResponse;
import com.click.apigateway.dto.summarization.SummarizationResponse;
import com.click.apigateway.dto.transcript.TranscriptResponse;
import com.click.apigateway.dto.youtubesearch.VideoSearchResponse;

import java.util.List;

public record SearchResponse(
        String originalQuery,
        String detectedCategory,
        // The final curated output — this is what the frontend will display
        List<CurationResponse.CuratedCard> curatedPicks,
        // Intermediate pipeline data — useful for debugging and demos
        int totalUniqueVideosFound,
        List<String> expandedQueries,
        List<VideoSearchResponse.VideoResult> videos,
        List<TranscriptResponse> transcripts,
        List<SummarizationResponse> summarizations
) {
}
