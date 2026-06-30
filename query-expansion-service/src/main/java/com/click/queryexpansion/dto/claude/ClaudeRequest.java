package com.click.queryexpansion.dto.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Request body shape expected by Anthropic's /v1/messages endpoint.
 * Kept minimal - only the fields we actually use.
 */
public record ClaudeRequest(
        String model,
        int max_tokens,
        List<ClaudeMessage> messages,
        Double temperature
) {
    public record ClaudeMessage(String role, String content) {
    }
}

