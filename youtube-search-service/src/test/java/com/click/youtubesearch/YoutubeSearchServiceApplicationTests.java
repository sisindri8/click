package com.click.youtubesearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "youtube.api.key=test-key-not-real")
class YoutubeSearchServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
