package com.click.apigateway.service;

import com.click.apigateway.config.PipelineProperties;
import com.click.apigateway.dto.SearchResponse;
import com.click.apigateway.dto.queryexpansion.QueryExpansionRequest;
import com.click.apigateway.dto.queryexpansion.QueryExpansionResponse;
import com.click.apigateway.dto.curation.CurationRequest;
import com.click.apigateway.dto.curation.CurationResponse;
import com.click.apigateway.dto.summarization.SummarizationRequest;
import com.click.apigateway.dto.summarization.SummarizationResponse;
import com.click.apigateway.dto.transcript.TranscriptRequest;
import com.click.apigateway.dto.transcript.TranscriptResponse;
import com.click.apigateway.dto.youtubesearch.VideoSearchRequest;
import com.click.apigateway.dto.youtubesearch.VideoSearchResponse;
import com.click.apigateway.exception.DownstreamServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the real pipeline:
 *
 *   user query
 *        |
 *        v
 *   Query Expansion Service  (1 query -> 5 variations + category)
 *        |
 *        v
 *   YouTube Search Service   (5 variations -> deduplicated, filtered, ranked videos)
 *        |
 *        v
 *   Transcript Service       (top N ranked videos -> Whisper transcripts)
 *   [only the top N - Whisper is slow (10-90s/video), fetching all videos'
 *    transcripts sequentially would make a single search take minutes]
 *        |
 *        v
 *   combined response back to caller
 *
 * This is intentionally a plain, synchronous, blocking chain (.block() calls)
 * for now - readable and easy to debug while we're proving the pipeline
 * works. Worth revisiting for parallelism (e.g. CompletableFuture to fetch
 * multiple transcripts concurrently) once Summarization Service joins the
 * chain and per-search latency starts to matter more.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchOrchestrationService {

    private final WebClient queryExpansionWebClient;
    private final WebClient youtubeSearchWebClient;
    private final WebClient transcriptWebClient;
    private final WebClient summarizationWebClient;
    private final WebClient curationWebClient;
    private final PipelineProperties pipelineProperties;

    public SearchResponse search(String userQuery) {
        QueryExpansionResponse expansion = callQueryExpansion(userQuery);
        VideoSearchResponse videoResults = callYoutubeSearch(userQuery, expansion.expandedQueries(), expansion.detectedCategory());
        List<TranscriptResponse> transcripts = fetchTopVideoTranscripts(videoResults.videos());
        List<SummarizationResponse> summarizations = summarizeTranscripts(transcripts, videoResults.videos());
        List<CurationResponse.CuratedCard> curatedPicks = curate(userQuery, expansion.detectedCategory(), summarizations);

        return new SearchResponse(
                userQuery,
                expansion.detectedCategory(),
                curatedPicks,
                videoResults.totalUniqueVideosFound(),
                expansion.expandedQueries(),
                videoResults.videos(),
                transcripts,
                summarizations
        );
    }

    private QueryExpansionResponse callQueryExpansion(String userQuery) {
        log.info("Step 1/3 -> calling Query Expansion Service for: '{}'", userQuery);
        try {
            QueryExpansionResponse response = queryExpansionWebClient.post()
                    .uri("/api/v1/query-expansion/expand")
                    .bodyValue(new QueryExpansionRequest(userQuery))
                    .retrieve()
                    .bodyToMono(QueryExpansionResponse.class)
                    .block();

            if (response == null || response.expandedQueries() == null || response.expandedQueries().isEmpty()) {
                throw new DownstreamServiceException("Query Expansion Service returned no query variations");
            }

            log.info("Query Expansion returned {} variations, category='{}'",
                    response.expandedQueries().size(), response.detectedCategory());
            return response;

        } catch (DownstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Query Expansion Service call failed", e);
            throw new DownstreamServiceException(
                    "Could not reach Query Expansion Service - is it running on the configured port?", e);
        }
    }

    private VideoSearchResponse callYoutubeSearch(String originalQuery, List<String> expandedQueries, String category) {
        log.info("Step 2/3 -> calling YouTube Search Service with {} query variations (category={})",
                expandedQueries.size(), category);
        try {
            VideoSearchResponse response = youtubeSearchWebClient.post()
                    .uri("/api/v1/youtube-search/search")
                    .bodyValue(new VideoSearchRequest(originalQuery, expandedQueries, category))
                    .retrieve()
                    .bodyToMono(VideoSearchResponse.class)
                    .block();

            if (response == null) {
                throw new DownstreamServiceException("YouTube Search Service returned an empty response");
            }

            log.info("YouTube Search returned {} relevant videos", response.totalUniqueVideosFound());
            return response;

        } catch (DownstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("YouTube Search Service call failed", e);
            throw new DownstreamServiceException(
                    "Could not reach YouTube Search Service - is it running on the configured port?", e);
        }
    }

    /**
     * Fetches transcripts only for the top N highest-ranked videos (videos
     * are already sorted by rankScore descending when they arrive here from
     * YouTube Search Service). A failure on ONE video's transcript does not
     * fail the whole search - we log it and continue with whatever
     * transcripts did succeed, since a partial result is far more useful to
     * the caller than a total failure over one bad video.
     */
    private List<TranscriptResponse> fetchTopVideoTranscripts(List<VideoSearchResponse.VideoResult> rankedVideos) {
        int topN = Math.min(pipelineProperties.transcriptTopNVideos(), rankedVideos.size());
        log.info("Step 3/3 -> fetching transcripts for top {} of {} ranked videos", topN, rankedVideos.size());

        List<TranscriptResponse> transcripts = new ArrayList<>();

        for (int i = 0; i < topN; i++) {
            String videoId = rankedVideos.get(i).videoId();
            try {
                TranscriptResponse transcript = transcriptWebClient.post()
                        .uri("/api/v1/transcript/fetch")
                        .bodyValue(new TranscriptRequest(videoId))
                        .retrieve()
                        .bodyToMono(TranscriptResponse.class)
                        .block();

                if (transcript != null) {
                    transcripts.add(transcript);
                    log.info("Transcript fetched for video {} ({} words, source={})",
                            videoId, transcript.wordCount(), transcript.source());
                }
            } catch (Exception e) {
                // Deliberately NOT rethrown - one video's transcript failing (e.g.
                // Whisper timeout, audio download blocked) shouldn't fail the entire
                // search. We skip it and move to the next video.
                log.warn("Transcript fetch failed for video {}, skipping it: {}", videoId, e.getMessage());
            }
        }

        return transcripts;
    }

    /**
     * Sends each transcript to Summarization Service to extract structured
     * product data. Looks up the channel name from the video list so Claude
     * has that context. A failure on one video's summarization is isolated -
     * same pattern as transcript fetching: partial results beat total failure.
     */
    private List<SummarizationResponse> summarizeTranscripts(
            List<TranscriptResponse> transcripts,
            List<VideoSearchResponse.VideoResult> videos) {

        if (transcripts.isEmpty()) {
            return List.of();
        }

        Map<String, String> channelByVideoId = videos.stream()
                .collect(java.util.stream.Collectors.toMap(
                        VideoSearchResponse.VideoResult::videoId,
                        VideoSearchResponse.VideoResult::channelName,
                        (a, b) -> a
                ));

        log.info("Step 4/4 -> summarizing {} transcripts", transcripts.size());
        List<SummarizationResponse> summarizations = new ArrayList<>();

        for (TranscriptResponse transcript : transcripts) {
            try {
                String channelName = channelByVideoId.getOrDefault(transcript.videoId(), "Unknown");

                SummarizationResponse summarization = summarizationWebClient.post()
                        .uri("/api/v1/summarization/summarize")
                        .bodyValue(new SummarizationRequest(
                                transcript.videoId(),
                                transcript.transcript(),
                                channelName))
                        .retrieve()
                        .bodyToMono(SummarizationResponse.class)
                        .block();

                if (summarization != null) {
                    summarizations.add(summarization);
                    log.info("Summarization complete for video {}: {} products extracted",
                            transcript.videoId(), summarization.products().size());
                }
            } catch (Exception e) {
                log.warn("Summarization failed for video {}, skipping: {}", transcript.videoId(), e.getMessage());
            }
        }

        return summarizations;
    }

    private List<CurationResponse.CuratedCard> curate(
            String originalQuery,
            String detectedCategory,
            List<SummarizationResponse> summarizations) {

        if (summarizations.isEmpty()) {
            log.warn("Skipping curation — no summarizations available");
            return List.of();
        }

        log.info("Step 5/5 -> calling Curation Service");

        List<CurationRequest.VideoSummary> videoSummaries = summarizations.stream()
                .map(s -> new CurationRequest.VideoSummary(
                        s.videoId(),
                        s.channelName(),
                        s.products().stream()
                                .map(p -> new CurationRequest.ExtractedProduct(
                                        p.name(), p.price(), p.pros(), p.cons(),
                                        p.recommended(), p.recommendationReason()))
                                .toList()
                ))
                .toList();

        try {
            CurationResponse response = curationWebClient.post()
                    .uri("/api/v1/curation/curate")
                    .bodyValue(new CurationRequest(originalQuery, detectedCategory, videoSummaries))
                    .retrieve()
                    .bodyToMono(CurationResponse.class)
                    .block();

            if (response == null || response.curatedPicks() == null) {
                log.warn("Curation Service returned empty response");
                return List.of();
            }

            log.info("Curation complete: {} curated cards", response.curatedPicks().size());
            return response.curatedPicks();

        } catch (Exception e) {
            log.error("Curation Service call failed, returning empty picks: {}", e.getMessage());
            return List.of();
        }
    }
}
