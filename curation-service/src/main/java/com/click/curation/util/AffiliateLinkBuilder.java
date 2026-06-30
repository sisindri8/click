package com.click.curation.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds Amazon India affiliate search links.
 * <p>
 * IMPORTANT — what this does and doesn't do:
 * This builds a SEARCH link (amazon.in/s?k=...), not a direct product page
 * link. Reliably resolving "Samsung Galaxy A55" to the exact right Amazon
 * product listing (ASIN) would require Amazon's Product Advertising API,
 * which has its own requirements (qualifying sales within 180 days, request
 * signing, strict rate limits) - see project notes for why that's deferred.
 * <p>
 * A search link still earns commission: Amazon's affiliate cookie attributes
 * ANY purchase made within 24 hours of the click to your tag, not just the
 * specific product searched for. This is a legitimate, commonly-used
 * affiliate pattern - not a workaround.
 */
public final class AffiliateLinkBuilder {

    private AffiliateLinkBuilder() {
    }

    private static final String AMAZON_BASE_URL = "https://www.amazon.in/s";

    /**
     * @param productName the product name extracted by Claude (e.g. "Samsung Galaxy A55")
     * @param associateTag your Amazon Associates tracking ID (e.g. "prodexa-21")
     * @return a full Amazon India search URL with the affiliate tag attached,
     *         or null if productName is blank (nothing meaningful to search for)
     */
    public static String buildSearchLink(String productName, String associateTag) {
        if (productName == null || productName.isBlank() || associateTag == null || associateTag.isBlank()) {
            return null;
        }

        String encodedQuery = URLEncoder.encode(productName.trim(), StandardCharsets.UTF_8);
        return AMAZON_BASE_URL + "?k=" + encodedQuery + "&tag=" + associateTag;
    }
}
