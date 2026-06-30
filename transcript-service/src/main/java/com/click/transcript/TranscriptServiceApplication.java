package com.click.transcript;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Transcript Service
 * <p>
 * Given a YouTube video ID, returns a clean transcript. The actual
 * download + speech-to-text work happens in a Python/Whisper script
 * (python/fetch_transcript.py), invoked as a subprocess - Whisper has no
 * mature pure-Java equivalent, so this is the one polyglot service in
 * the system by necessity, not by choice.
 */
@SpringBootApplication
public class TranscriptServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TranscriptServiceApplication.class, args);
    }
}
