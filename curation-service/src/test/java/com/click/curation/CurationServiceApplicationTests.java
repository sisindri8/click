package com.click.curation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "claude.api.key=test-key-not-real")
class CurationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
