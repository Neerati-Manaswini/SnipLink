package com.manaswini.sniplink;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Validation and clean-up rules for the URLs and aliases people submit. */
public final class UrlRules {

    public static final int MAX_URL_LENGTH = 2048;
    private static final Pattern ALIAS = Pattern.compile("[A-Za-z0-9_-]{3,32}");
    private static final Set<String> RESERVED = Set.of("api", "error", "h2-console", "admin", "static", "links", "favicon");

    private UrlRules() {}

    /**
     * Returns a clean http/https URL. Adds https:// when the scheme is missing, and
     * rejects other schemes (javascript:, data:, ...), hosts without a dot, and links
     * that point back to this service (which would create redirect loops).
     */
    public static String normalizeUrl(String raw, String ownHost) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Enter a URL to shorten.");
        }
        String url = raw.trim();
        if (!url.contains("://")) {
            url = "https://" + url;
        }
        if (url.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("That URL is too long.");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("That doesn't look like a valid URL.");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http:// and https:// links can be shortened.");
        }
        String host = uri.getHost();
        if (host == null || !host.contains(".")) {
            throw new IllegalArgumentException("That doesn't look like a valid URL.");
        }
        if (ownHost != null && host.equalsIgnoreCase(ownHost)) {
            throw new IllegalArgumentException("That link already points to this service.");
        }
        return url;
    }

    /** Returns null when no alias was given; otherwise a validated alias. */
    public static String normalizeAlias(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String alias = raw.trim();
        if (!ALIAS.matcher(alias).matches()) {
            throw new IllegalArgumentException("Alias must be 3-32 characters: letters, numbers, - or _.");
        }
        if (RESERVED.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("That alias is reserved. Please choose another.");
        }
        return alias;
    }
}
