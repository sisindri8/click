package com.click.queryexpansion.exception;

/**
 * Thrown when the Claude API call fails, times out, or returns a response
 * we can't parse into the expected JSON shape.
 */
public class ClaudeApiException extends RuntimeException {

    public ClaudeApiException(String message) {
        super(message);
    }

    public ClaudeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
