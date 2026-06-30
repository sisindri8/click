package com.click.queryexpansion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Query Expansion Service
 * <p>
 * Takes a single user search query (e.g. "best phones under 30k") and uses
 * Claude API to generate 5 semantically similar query variations. This
 * widens the net when we search YouTube, since different reviewers phrase
 * the same topic differently.
 */
@SpringBootApplication
public class QueryExpansionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryExpansionServiceApplication.class, args);
    }
}
