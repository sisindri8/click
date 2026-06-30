package com.click.youtubesearch.util;

import java.time.Duration;

/**
 * YouTube returns video duration as an ISO-8601 duration string, e.g.
 * "PT12M34S" = 12 minutes 34 seconds. java.time.Duration.parse() handles
 * this format natively, so this is just a thin, defensive wrapper.
 */
public final class DurationParser {

    private DurationParser() {
    }

    public static int toSeconds(String iso8601Duration) {
        if (iso8601Duration == null || iso8601Duration.isBlank()) {
            return 0;
        }
        try {
            return (int) Duration.parse(iso8601Duration).toSeconds();
        } catch (Exception e) {
            return 0; // treat unparsable durations as 0 so they get filtered out, not crash the pipeline
        }
    }
}
