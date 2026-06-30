package com.click.curation.service;

import com.click.curation.dto.CurationRequest;
import com.click.curation.dto.CurationResponse;
import com.click.curation.dto.CurationResponse.CuratedCard;
import com.click.curation.dto.claude.ClaudeRequest;
import com.click.curation.dto.claude.ClaudeResponse;
import com.click.curation.exception.CurationException;
import com.click.curation.util.AffiliateLinkBuilder;
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
public class CurationService {

    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper;

    @Value("${claude.mock-mode:false}")
    private boolean mockMode;

    @Value("${claude.model.haiku}")
    private String model;

    @Value("${affiliate.amazon.associate-tag}")
    private String amazonAssociateTag;

    public CurationResponse curate(CurationRequest request) {
        if (mockMode) {
            log.warn("MOCK MODE ACTIVE - returning mock curated cards");
            return mockCuration(request);
        }

        int totalProducts = request.summarizations().stream()
                .mapToInt(s -> s.products().size())
                .sum();

        log.info("Curating {} products from {} videos for query '{}'",
                totalProducts, request.summarizations().size(), request.originalQuery());

        if (totalProducts == 0) {
            throw new CurationException("No products to curate - all summarizations were empty");
        }

        String prompt = buildPrompt(request);
        ClaudeRequest claudeRequest = new ClaudeRequest(
                model,
                2000,
                List.of(new ClaudeRequest.Message("user", prompt))
        );

        ClaudeResponse response = claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(claudeRequest)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .block();

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new CurationException("Claude returned empty response during curation");
        }

        String rawText = response.content().get(0).text();
        List<CuratedCard> cards = parseCards(rawText);

        log.info("Curation complete: {} cards produced for '{}'", cards.size(), request.originalQuery());
        return new CurationResponse(
                request.originalQuery(),
                request.detectedCategory(),
                request.summarizations().size(),
                cards,
                "claude"
        );
    }

    /**
     * The grouping prompt — this is what makes the output actually useful.
     * Key decisions:
     * - Pass ALL product data in one call (cheaper than multiple calls)
     * - Max 5 categories, dynamically chosen based on what's in the data
     * - Force different products per category to avoid all cards showing same phone
     * - Confidence scoring based on how many sources mentioned a product
     * - Return JSON only — no prose, no markdown
     */
    private String buildPrompt(CurationRequest request) {
        StringBuilder productsJson = new StringBuilder();
        for (CurationRequest.VideoSummary summary : request.summarizations()) {
            for (CurationRequest.ExtractedProduct product : summary.products()) {
                productsJson.append(String.format(
                        "Channel: %s | Product: %s | Price: %s | Pros: %s | Cons: %s | Recommended: %s\n",
                        summary.channelName(),
                        product.name(),
                        product.price() != null ? product.price() : "unknown",
                        String.join(", ", product.pros()),
                        String.join(", ", product.cons()),
                        product.recommended() ? "YES - " + product.recommendationReason() : "NO"
                ));
            }
        }

        return """
                You are a tech product curator for Indian consumers.
                
                User searched for: "%s"
                Product category: %s
                
                Products extracted from trusted Telugu tech YouTube reviews:
                %s
                
                Group these into MAX 5 curated picks. Choose categories that make sense for this query from:
                Best Overall (🏆), Best Camera Phone (📸), Best Battery Life (🔋), Best Performance (⚡), Best Value for Money (💰)
                
                Rules:
                - Pick ONE winner per category - the product most mentioned or most recommended for that use case
                - Use different products for different categories where possible
                - confidence: HIGH if mentioned by 2+ sources, MEDIUM if 1 source recommended it, LOW if inferred
                - keyReasons: exactly 3 short bullet points why this won its category
                - verdict: one punchy sentence (max 12 words)
                - Only include categories that genuinely apply to the data
                - Return ONLY valid JSON, no markdown, no preamble
                
                Schema:
                {"curatedPicks": [{"category": "string", "emoji": "string", "productName": "string", "price": "string", "confidence": "HIGH|MEDIUM|LOW", "keyReasons": ["string","string","string"], "verdict": "string", "channelName": "string", "videoUrl": null}]}
                """.formatted(
                request.originalQuery(),
                request.detectedCategory() != null ? request.detectedCategory() : "general tech",
                productsJson
        );
    }

    private List<CuratedCard> parseCards(String rawText) {
        try {
            String cleaned = rawText.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```(json)?", "").trim();
                if (cleaned.endsWith("```")) {
                    cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
                }
            }

            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode picks = root.path("curatedPicks");

            List<CuratedCard> cards = new ArrayList<>();
            for (JsonNode node : picks) {
                List<String> keyReasons = new ArrayList<>();
                node.path("keyReasons").forEach(r -> keyReasons.add(r.asText()));

                String productName = node.path("productName").asText();
                String buyUrl = AffiliateLinkBuilder.buildSearchLink(productName, amazonAssociateTag);

                cards.add(new CuratedCard(
                        node.path("category").asText(),
                        node.path("emoji").asText("🏆"),
                        productName,
                        node.path("price").asText(null),
                        node.path("confidence").asText("MEDIUM"),
                        keyReasons,
                        node.path("verdict").asText(null),
                        node.path("channelName").asText(null),
                        node.path("videoUrl").asText(null),
                        buyUrl
                ));
            }
            return cards;

        } catch (Exception e) {
            log.error("Failed to parse Claude curation response: {}", rawText, e);
            throw new CurationException("Could not parse Claude curation response", e);
        }
    }

    private CurationResponse mockCuration(CurationRequest request) {
        List<CuratedCard> mockCards = List.of(
                new CuratedCard(
                        "Best Overall", "🏆",
                        "Samsung Galaxy A55", "₹27,999", "HIGH",
                        List.of("Best display in segment", "Solid build quality", "Reliable day-to-day performance"),
                        "The safest all-round buy under 30k.",
                        "Prasad Tech in Telugu", null,
                        AffiliateLinkBuilder.buildSearchLink("Samsung Galaxy A55", amazonAssociateTag)
                ),
                new CuratedCard(
                        "Best Performance", "⚡",
                        "POCO F7", "₹26,999", "HIGH",
                        List.of("Snapdragon flagship chip", "Best gaming performance under 30k", "Smooth 120Hz display"),
                        "Unbeatable raw power for the price.",
                        "besttechintelugu", null,
                        AffiliateLinkBuilder.buildSearchLink("POCO F7", amazonAssociateTag)
                ),
                new CuratedCard(
                        "Best Value for Money", "💰",
                        "iQOO Neo 10", "₹24,999", "MEDIUM",
                        List.of("Flagship specs at mid-range price", "Good battery life", "Decent camera"),
                        "Most phone for the least money.",
                        "Prasad Tech in Telugu", null,
                        AffiliateLinkBuilder.buildSearchLink("iQOO Neo 10", amazonAssociateTag)
                )
        );

        return new CurationResponse(
                request.originalQuery(),
                request.detectedCategory(),
                request.summarizations().size(),
                mockCards,
                "mock"
        );
    }
}
