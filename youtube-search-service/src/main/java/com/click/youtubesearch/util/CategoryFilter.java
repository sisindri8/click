package com.click.youtubesearch.util;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Scores how well a video title matches a detected product category.
 * <p>
 * This catches a gap RelevanceFilter can't: a query like "best phones under
 * 30k" generates expanded variations that all contain "phone"/"mobile", but
 * a channel might still surface a laptop video that happens to share other
 * keywords (e.g. "best" + "30k" + "budget"). Category matching adds a second,
 * independent signal - does the title mention ANY synonym of the category
 * itself - which is harder for an unrelated video to accidentally satisfy.
 * <p>
 * This is intentionally a soft signal (contributes to ranking), not a hard
 * filter - a category misdetection by Query Expansion shouldn't silently
 * wipe out all results.
 */
public final class CategoryFilter {

    private CategoryFilter() {
    }

    private static final Map<String, Set<String>> CATEGORY_SYNONYMS = Map.of(
            "mobiles", Set.of("phone", "phones", "smartphone", "smartphones", "mobile", "mobiles"),
            "laptops", Set.of("laptop", "laptops", "notebook", "ultrabook"),
            "audio", Set.of("earbuds", "earphone", "earphones", "headphone", "headphones", "speaker", "speakers", "tws"),
            "wearables", Set.of("smartwatch", "watch", "band", "fitness band"),
            "tv", Set.of("tv", "television", "smarttv")
    );

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]+");

    /**
     * Returns 1 if the title mentions the category, 0 if it doesn't mention
     * it but the category is known, or -1 if category itself is unrecognized
     * (so callers can choose to ignore this signal entirely in that case).
     */
    public static int score(String category, String videoTitle) {
        if (category == null || videoTitle == null) {
            return -1;
        }

        Set<String> synonyms = CATEGORY_SYNONYMS.get(category.toLowerCase());
        if (synonyms == null) {
            return -1; // unrecognized category (e.g. "other") - no signal available
        }

        String normalizedTitle = " " + NON_ALPHANUMERIC.matcher(videoTitle.toLowerCase()).replaceAll(" ") + " ";

        for (String synonym : synonyms) {
            if (normalizedTitle.contains(" " + synonym + " ")) {
                return 1;
            }
        }
        return 0;
    }
}
