package com.click.curation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Curation Service — the final step in the Click pipeline.
 * <p>
 * Takes all extracted product data from multiple video summaries and uses
 * Claude to intelligently group them into max 5 curated cards by use case.
 * This is what the user actually sees: "Best Overall", "Best Camera", etc.
 */
@SpringBootApplication
public class CurationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurationServiceApplication.class, args);
    }
}
