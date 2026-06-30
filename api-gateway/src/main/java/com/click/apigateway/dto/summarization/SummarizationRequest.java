package com.click.apigateway.dto.summarization;

import java.util.List;

public record SummarizationRequest(
        String videoId,
        String transcript,
        String channelName
) {
}
