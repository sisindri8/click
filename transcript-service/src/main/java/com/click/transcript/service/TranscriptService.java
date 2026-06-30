package com.click.transcript.service;

import com.click.transcript.dto.TranscriptResponse;
import com.click.transcript.exception.TranscriptFetchException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Bridges Java and the Python/Whisper transcription script.
 * <p>
 * Java starts the Python process, captures its stdout (which is reserved
 * for exactly one line of JSON - all Python-side logging goes to stderr
 * so it doesn't corrupt what we parse here), and converts that into a
 * TranscriptResponse.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final ObjectMapper objectMapper;

    @Value("${transcript.mock-mode:false}")
    private boolean mockMode;

    @Value("${transcript.python.executable}")
    private String pythonExecutable;

    @Value("${transcript.python.script-path}")
    private String scriptPath;

    @Value("${transcript.python.timeout-seconds}")
    private int timeoutSeconds;

    /**
     * When transcript.mock-mode=true (see application.yml), this skips the
     * real Whisper call entirely and returns fake transcript text. Lets you
     * verify the whole Java pipeline (controller, validation, downstream
     * wiring) without installing Whisper/ffmpeg/yt-dlp first.
     */
    public TranscriptResponse fetchTranscript(String videoId) {
        if (mockMode) {
            log.warn("MOCK MODE ACTIVE - returning fake transcript, Whisper was NOT actually called");
            return mockTranscript(videoId);
        }

        log.info("Fetching real transcript for video {} via Whisper (this can take 10-90 seconds)...", videoId);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath, videoId);
            processBuilder.redirectErrorStream(false); // keep stdout (JSON) separate from stderr (logs)

            Process process = processBuilder.start();

            // IMPORTANT: stdout and stderr must be drained CONCURRENTLY, not
            // sequentially. yt-dlp/Whisper write a lot of progress output to
            // stderr - if Java blocks reading stdout first, the stderr pipe
            // buffer fills up, Python's stderr writes block, and Python never
            // reaches the final stdout print. That deadlock is what was
            // happening here (Python worked fine standalone - this was a
            // Java-side bug, not a Python problem).
            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStreamQuietly(process.getInputStream()));
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStreamQuietly(process.getErrorStream()));

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TranscriptFetchException(
                        "Transcript fetch timed out after " + timeoutSeconds + "s for video " + videoId);
            }

            String stdout = stdoutFuture.join();
            String stderr = stderrFuture.join();

            if (!stderr.isBlank()) {
                log.debug("Python script log output for {}:\n{}", videoId, stderr);
            }

            return parsePythonOutput(videoId, stdout);

        } catch (TranscriptFetchException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to invoke Python transcript script for video {}", videoId, e);
            throw new TranscriptFetchException(
                    "Could not run Python transcript script - is Python installed and on PATH?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptFetchException("Transcript fetch was interrupted for video " + videoId, e);
        }
    }

    private TranscriptResponse parsePythonOutput(String videoId, String stdout) {
        if (stdout == null || stdout.isBlank()) {
            throw new TranscriptFetchException("Python script produced no output for video " + videoId);
        }

        try {
            JsonNode node = objectMapper.readTree(stdout.trim());

            if (node.has("error")) {
                String details = node.path("details").asText("no details provided");
                throw new TranscriptFetchException(
                        "Transcript unavailable for video " + videoId + ": " + details);
            }

            String transcript = node.path("transcript").asText();
            String source = node.path("source").asText("unknown");

            if (transcript.isBlank()) {
                throw new TranscriptFetchException("Python script returned an empty transcript for video " + videoId);
            }

            int wordCount = transcript.trim().split("\\s+").length;
            return new TranscriptResponse(videoId, transcript, source, wordCount);

        } catch (TranscriptFetchException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not parse Python script output for video {}: {}", videoId, stdout, e);
            throw new TranscriptFetchException("Could not parse transcript script output", e);
        }
    }

    /**
     * Wraps readStream's checked IOException as unchecked so this can run
     * inside CompletableFuture.supplyAsync (which only accepts Supplier,
     * no checked exceptions allowed in its signature).
     */
    private String readStreamQuietly(InputStream inputStream) {
        try {
            return readStream(inputStream);
        } catch (IOException e) {
            log.warn("Error reading process stream", e);
            return "";
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private TranscriptResponse mockTranscript(String videoId) {
        String fakeTranscript = "This is a mock transcript for testing purposes. " +
                "In a real run, Whisper would transcribe the actual audio of video " + videoId + ". " +
                "This phone has a great display, solid battery life, and a capable camera for the price. " +
                "Overall it offers good value for money in this segment.";
        int wordCount = fakeTranscript.trim().split("\\s+").length;
        return new TranscriptResponse(videoId, fakeTranscript, "mock", wordCount);
    }
}
