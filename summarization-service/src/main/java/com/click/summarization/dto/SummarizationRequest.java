package com.click.summarization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SummarizationRequest(

        @NotBlank(message = "videoId must not be blank")
        String videoId,

        @NotBlank(message = "transcript must not be blank")
        @Size(max = 50000, message = "transcript too long - trim before sending")
        String transcript,

        // Channel name is passed through so Claude knows the context
        // (e.g. "Prasad Tech in Telugu") - helps it understand mixed-language content
        String channelName

) {
}
