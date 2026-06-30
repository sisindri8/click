package com.click.curation.exception;

public class CurationException extends RuntimeException {
    public CurationException(String message) { super(message); }
    public CurationException(String message, Throwable cause) { super(message, cause); }
}
