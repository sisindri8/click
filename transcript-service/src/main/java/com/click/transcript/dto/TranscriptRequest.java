package com.click.transcript.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TranscriptRequest(

        @NotBlank(message = "videoId must not be blank")
        @Pattern(regexp = "^[a-zA-Z0-9_-]{11}$", message = "videoId must be a valid 11-character YouTube video ID")
        String videoId

) {
}
