package com.click.summarization.service;

import com.click.summarization.dto.SummarizationRequest;
import com.click.summarization.dto.SummarizationResponse;
import com.click.summarization.dto.SummarizationResponse.ExtractedProduct;
import com.click.summarization.dto.claude.ClaudeRequest;
import com.click.summarization.dto.claude.ClaudeResponse;
import com.click.summarization.exception.SummarizationException;
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
public class SummarizationService {

    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper;

    @Value("${claude.mock-mode:false}")
    private boolean mockMode;

    @Value("${claude.model.haiku}")
    private String model;

    public SummarizationResponse summarize(SummarizationRequest request) {
        if (mockMode) {
            log.warn("MOCK MODE ACTIVE - returning fake product data, Claude was NOT called");
            return mockSummarization(request);
        }

        log.info("Summarizing transcript for video '{}' ({} chars)", request.videoId(), request.transcript().length());

        String trimmedTranscript = trimTranscript(request.transcript());
        String prompt = buildPrompt(trimmedTranscript, request.channelName());

        ClaudeRequest claudeRequest = new ClaudeRequest(
                model,
                1500,
                List.of(new ClaudeRequest.Message("user", prompt))
        );

        ClaudeResponse response = claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(claudeRequest)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .block();

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new SummarizationException("Claude returned empty response for video " + request.videoId());
        }

        String rawText = response.content().get(0).text();
        List<ExtractedProduct> products = parseProducts(request.videoId(), rawText);

        log.info("Extracted {} products from video '{}'", products.size(), request.videoId());
        return new SummarizationResponse(request.videoId(), request.channelName(), products, "claude");
    }

    /**
     * The extraction prompt — this is the most important part of this service.
     * Key design decisions:
     * - Short, direct instructions (fewer tokens = cheaper + faster)
     * - Strict JSON-only output instruction (no markdown fences, no preamble)
     * - Cap on pros/cons count so output stays concise and parseable
     * - Explicit instruction to handle mixed Telugu/English content
     * - "If unclear, skip" instruction prevents hallucination on vague transcripts
     */
    private String buildPrompt(String transcript, String channelName) {
        return """
                Extract product reviews from this YouTube tech review transcript.
                Channel: %s
                
                Rules:
                - Extract ONLY products that are clearly reviewed (phone, laptop, earbuds, etc.)
                - Price must be in INR - extract as-is (e.g. "27,999" or "27k")
                - Pros: max 3 short points
                - Cons: max 2 short points
                - recommended: true only if reviewer explicitly recommends buying it
                - recommendationReason: one short sentence, null if not recommended
                - If transcript is unclear or mostly non-English, extract what you can
                - Return ONLY valid JSON, no markdown, no preamble
                
                Schema:
                {"products": [{"name": "string", "price": "string or null", "pros": ["string"], "cons": ["string"], "recommended": boolean, "recommendationReason": "string or null"}]}
                
                Transcript:
                %s
                """.formatted(channelName != null ? channelName : "Unknown", transcript);
    }

    /**
     * Trims transcript to a safe token budget before sending to Claude.
     * A 10-minute video transcript is ~1500-2000 words. At ~1.3 tokens/word,
     * that's ~2000-2600 tokens of input. We cap at 6000 chars (~1500 words)
     * which keeps us well within Haiku's context window and controls cost.
     * The middle section is kept since intros/outros are already trimmed by
     * the Python audio-trimming step - what arrives here is already the
     * review content, so we just take the first N chars.
     */
    private String trimTranscript(String transcript) {
        int maxChars = 6000;
        if (transcript.length() <= maxChars) {
            return transcript;
        }
        log.debug("Trimming transcript from {} to {} chars", transcript.length(), maxChars);
        return transcript.substring(0, maxChars);
    }

    private List<ExtractedProduct> parseProducts(String videoId, String rawText) {
        try {
            String cleaned = stripMarkdownFences(rawText.trim());
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode productsNode = root.path("products");

            List<ExtractedProduct> products = new ArrayList<>();
            for (JsonNode node : productsNode) {
                List<String> pros = new ArrayList<>();
                List<String> cons = new ArrayList<>();

                node.path("pros").forEach(p -> pros.add(p.asText()));
                node.path("cons").forEach(c -> cons.add(c.asText()));

                products.add(new ExtractedProduct(
                        node.path("name").asText(null),
                        node.path("price").asText(null),
                        pros,
                        cons,
                        node.path("recommended").asBoolean(false),
                        node.path("recommendationReason").asText(null)
                ));
            }
            return products;

        } catch (Exception e) {
            log.error("Failed to parse Claude product extraction for video {}: {}", videoId, rawText, e);
            throw new SummarizationException("Could not parse Claude product extraction response", e);
        }
    }

    private String stripMarkdownFences(String text) {
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(json)?", "").trim();
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3).trim();
            }
        }
        return text;
    }

    private SummarizationResponse mockSummarization(SummarizationRequest request) {
        // Return mock products that match the video context (channel name or transcript hint)
        String context = (request.channelName() + " " + request.transcript()).toLowerCase();

        List<ExtractedProduct> mockProducts;

        if (context.contains("laptop") || context.contains("macbook") || context.contains("notebook")) {
            mockProducts = List.of(
                    new ExtractedProduct(
                            "ASUS VivoBook 15",
                            "₹45,999",
                            List.of("Best display for the price", "Solid build quality", "Good battery life"),
                            List.of("Average GPU performance"),
                            true,
                            "Best all-round laptop under 50k for students and professionals"
                    ),
                    new ExtractedProduct(
                            "HP Pavilion 15",
                            "₹48,999",
                            List.of("Great performance", "Good keyboard"),
                            List.of("Mediocre display", "Runs warm under load"),
                            false,
                            null
                    )
            );
        } else if (context.contains("earbuds") || context.contains("earphone") ||
                   context.contains("headphone") || context.contains("speaker")) {
            mockProducts = List.of(
                    new ExtractedProduct(
                            "boAt Airdopes 141",
                            "₹1,299",
                            List.of("Great sound for price", "Long battery life", "Comfortable fit"),
                            List.of("Average mic quality"),
                            true,
                            "Best value TWS earbuds under 2k"
                    )
            );
        } else {
            // Default: phones
            mockProducts = List.of(
                    new ExtractedProduct(
                            "Samsung Galaxy A55",
                            "₹27,999",
                            List.of("Best display in segment", "Solid build quality", "Reliable performance"),
                            List.of("Average battery life"),
                            true,
                            "Best all-round phone under 30k for most users"
                    ),
                    new ExtractedProduct(
                            "POCO F7",
                            "₹26,999",
                            List.of("Best performance in segment", "Good battery life"),
                            List.of("Average camera", "Plastic build"),
                            false,
                            null
                    )
            );
        }

        return new SummarizationResponse(request.videoId(), request.channelName(), mockProducts, "mock");
    }
}
