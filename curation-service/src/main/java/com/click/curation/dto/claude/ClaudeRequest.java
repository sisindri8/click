package com.click.curation.dto.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public record ClaudeRequest(
        String model,
        int max_tokens,
        List<Message> messages
) {
    public record Message(String role, String content) {}
}
