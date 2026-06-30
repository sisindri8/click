package com.click.youtubesearch.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scores video recency on a 0-50 sliding scale (matches the point scale
 * discussed in the roadmap: this week +50, this month +30, 3 months +10,
 * 6-12 months +0). Tech reviews age fast - prices change, newer models
 * launch - so a fresher video should outrank an older one when relevance
 * is otherwise similar, without being a hard cutoff (the max-age-months
 * filter already drops anything older than that at the YouTube API call
 * level via publishedAfter).
 */
public final class FreshnessScorer {

    private FreshnessScorer() {
    }

    public static int score(Instant publishedAt) {
        if (publishedAt == null) {
            return 0;
        }

        long daysOld = ChronoUnit.DAYS.between(publishedAt, Instant.now());

        if (daysOld <= 7) {
            return 50;
        } else if (daysOld <= 30) {
            return 30;
        } else if (daysOld <= 90) {
            return 10;
        } else {
            return 0; // 3-12 months old (anything older is already excluded upstream)
        }
    }
}
