package com.click.queryexpansion.dto.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Response shape returned by Anthropic's /v1/messages endpoint.
 * We only map the fields we need - "content" holds the actual text reply.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaudeResponse(
        String id,
        String model,
        List<ContentBlock> content,
        String stop_reason
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) {
    }
}
