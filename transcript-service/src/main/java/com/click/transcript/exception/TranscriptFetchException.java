package com.click.transcript.exception;

public class TranscriptFetchException extends RuntimeException {

    public TranscriptFetchException(String message) {
        super(message);
    }

    public TranscriptFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
