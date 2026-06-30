package com.click.queryexpansion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test - just verifies the Spring context loads without errors.
 * We override the Claude API key here since the real one shouldn't be
 * required to run tests, and CI environments won't have it set.
 */
@SpringBootTest
@TestPropertySource(properties = "claude.api.key=test-key-not-real")
class QueryExpansionServiceApplicationTests {

    @Test
    void contextLoads() {
        // If the Spring context fails to start, this test fails automatically.
    }
}
