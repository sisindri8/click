package com.click.queryexpansion.service;

import com.click.queryexpansion.dto.QueryExpansionResponse;
import com.click.queryexpansion.dto.claude.ClaudeRequest;
import com.click.queryexpansion.dto.claude.ClaudeResponse;
import com.click.queryexpansion.exception.ClaudeApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeQueryExpansionService {

    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper;

    @Value("${claude.model.haiku}")
    private String haikuModel;

    @Value("${claude.mock-mode:false}")
    private boolean mockMode;

    /**
     * Calls Claude (Haiku tier - this is a simple/cheap task, no need for Sonnet)
     * to expand a single query into 5 semantic variations, and detect the
     * product category in the same call to save tokens.
     * <p>
     * When claude.mock-mode=true (see application.yml), this skips the real
     * API call entirely and returns a fake but realistic response. This lets
     * you verify the whole Spring Boot app - controller, validation, JSON
     * shape - without needing a billed Claude API key yet.
     */
    public QueryExpansionResponse expand(String userQuery) {
        if (mockMode) {
            log.warn("MOCK MODE ACTIVE - returning fake data, no real Claude API call was made");
            return mockExpand(userQuery);
        }

        String prompt = buildPrompt(userQuery);

        ClaudeRequest request = new ClaudeRequest(
                haikuModel,
                500,
                List.of(new ClaudeRequest.ClaudeMessage("user", prompt)),
                0.7 // some creativity in phrasing variations, but still grounded
        );

        ClaudeResponse response = claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .block();

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new ClaudeApiException("Claude API returned an empty response for query: " + userQuery);
        }

        String rawText = response.content().get(0).text();
        return parseClaudeJson(userQuery, rawText);
    }

    /**
     * Generates a deterministic fake response so you can test the full
     * request/response cycle without spending anything. Remove or disable
     * mock-mode once your billing/key setup is confirmed working.
     */
    private QueryExpansionResponse mockExpand(String userQuery) {
        String query = userQuery.toLowerCase();
        String category;
        if (query.contains("laptop") || query.contains("notebook")) {
            category = "laptops";
        } else if (query.contains("earbuds") || query.contains("earphone") ||
                   query.contains("headphone") || query.contains("speaker") ||
                   query.contains("neckband")) {
            category = "audio";
        } else if (query.contains("watch") || query.contains("band")) {
            category = "wearables";
        } else if (query.contains("tv") || query.contains("television")) {
            category = "tv";
        } else {
            category = "mobiles";
        }

        List<String> fakeVariations = List.of(
                userQuery + " 2024",
                "best " + userQuery,
                userQuery + " review",
                "top " + userQuery,
                userQuery + " comparison"
        );
        return new QueryExpansionResponse(userQuery, fakeVariations, category);
    }

    private String buildPrompt(String userQuery) {
        // Strict, short prompt - deliberately low on filler words to keep input
        // tokens cheap (see cost optimization notes). Forces JSON-only output so
        // we don't have to do fragile text parsing.
        return """
                Generate 5 alternative search phrasings for this query, plus detect its product category.
                Query: "%s"

                Rules:
                - Variations must mean the same thing, just phrased differently (synonyms, reordering, common YouTube search style)
                - category must be a short lowercase word: mobiles, laptops, audio, wearables, tv, or other
                - Return ONLY valid JSON, no extra text, no markdown fences

                Schema:
                {"category": "string", "variations": ["string", "string", "string", "string", "string"]}
                """.formatted(userQuery);
    }

    private QueryExpansionResponse parseClaudeJson(String originalQuery, String rawText) {
        try {
            String cleaned = stripMarkdownFences(rawText);
            JsonNode node = objectMapper.readTree(cleaned);

            List<String> variations = new ArrayList<>();
            node.path("variations").forEach(v -> variations.add(v.asText()));

            String category = node.path("category").asText("other");

            if (variations.isEmpty()) {
                throw new ClaudeApiException("Claude returned no query variations for: " + originalQuery);
            }

            return new QueryExpansionResponse(originalQuery, variations, category);

        } catch (Exception e) {
            log.error("Failed to parse Claude response for query '{}': {}", originalQuery, rawText, e);
            throw new ClaudeApiException("Could not parse Claude response into expected JSON shape", e);
        }
    }

    /**
     * Claude sometimes wraps JSON in ```json ... ``` even when told not to.
     * Strip that defensively rather than failing the whole request.
     */
    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}
