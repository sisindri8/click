package com.click.summarization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Summarization Service
 * <p>
 * Takes a raw video transcript and sends it to Claude API with a structured
 * extraction prompt. Returns a list of products mentioned in the video, each
 * with price, pros, cons, and whether the reviewer recommended it.
 * <p>
 * This is where the pipeline goes from "raw text" to "structured product data"
 * that the Curation Service can then group and rank into final recommendations.
 */
@SpringBootApplication
public class SummarizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SummarizationServiceApplication.class, args);
    }
}
