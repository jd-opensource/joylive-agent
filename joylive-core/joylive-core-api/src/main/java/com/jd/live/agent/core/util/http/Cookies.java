package com.jd.live.agent.core.util.http;

import java.util.function.BiConsumer;

/**
 * A utility class for handling cookie.
 * Such as:
 *
 * <li>Set-Cookie: name=value; Path=/; Domain=example.com</li>
 * <li>Set-Cookie2: name=value; Version=1; Max-Age=3600; Path=/; Domain=example.com, name1=value1","; </li>
 * <li>name=value; Version=1; Max-Age=3600; Path=/; Domain=example.com</li>
 */
public class Cookies {

    private static final String SET_COOKIE_PREFIX = "set-cookie";
    private static final int SET_COOKIE_PREFIX_LENGTH = SET_COOKIE_PREFIX.length();
    private static final int SET_COOKIE_LENGTH = SET_COOKIE_PREFIX_LENGTH + 1;
    private static final int SET_COOKIE2_LENGTH = SET_COOKIE_PREFIX_LENGTH + 2;
    private static final String VERSION = "version";
    private static final int VERSION_LENGTH = VERSION.length();
    private static final String MAX_AGE = "max-age";
    private static final int MAX_AGE_LENGTH = MAX_AGE.length();

    /**
     * Parses the given HTTP header string and extracts cookies using the provided consumer.
     *
     * @param header   the HTTP header string to parse
     * @param consumer the consumer to process each parsed cookie
     */
    public static void parse(String header, BiConsumer<String, String> consumer) {
        // Check if header is null or empty
        if (header == null || header.trim().isEmpty() || consumer == null) {
            return;
        }

        header = header.trim();
        Boolean isVersionOne = null;

        // Check and strip off the set-cookie or set-cookie2 prefix if present
        // version0:"set-cookie:"
        // version1:"set-cookie2:"
        if (header.regionMatches(true, 0, SET_COOKIE_PREFIX, 0, SET_COOKIE_PREFIX_LENGTH)) {
            if (header.length() > SET_COOKIE_PREFIX_LENGTH) {
                if (header.charAt(SET_COOKIE_PREFIX_LENGTH) == ':') {
                    header = header.substring(SET_COOKIE_LENGTH).trim();
                    isVersionOne = false;
                } else if (header.regionMatches(true, SET_COOKIE_PREFIX_LENGTH, "2:", 0, 2)) {
                    header = header.substring(SET_COOKIE2_LENGTH).trim();
                    isVersionOne = true;
                }
            }
        }

        isVersionOne = isVersionOne == null ? !checkVersionOne(header) : isVersionOne;

        if (isVersionOne) {
            parseVersionOneCookies(header, consumer);
        } else {
            parseInternal(header, consumer);
        }
    }

    /**
     * Checks if the given header string contains version 1 cookies.
     *
     * @param header the header string to check
     * @return {@code true} if the header contains version 1 cookies, {@code false} otherwise
     */
    private static boolean checkVersionOne(String header) {
        boolean inQuotes = false;
        int len = header.length();

        for (int i = 0; i < len; i++) {
            char c = header.charAt(i);
            // Check for quotes to handle values with embedded semicolons or commas
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                // Check for version information and max-age while traversing
                if ((i + VERSION_LENGTH < len && header.regionMatches(true, i, VERSION, 0, VERSION_LENGTH)) ||
                        (i + MAX_AGE_LENGTH < len && header.regionMatches(true, i, MAX_AGE, 0, MAX_AGE_LENGTH))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Parses version 1 cookies from the given header string and processes them using the provided consumer.
     *
     * @param header   the header string to parse
     * @param consumer the consumer to process each parsed cookie
     */
    private static void parseVersionOneCookies(String header, BiConsumer<String, String> consumer) {
        boolean inQuotes = false;
        int len = header.length();
        int start = 0;

        for (int i = 0; i < len; i++) {
            char c = header.charAt(i);

            // Check for quotes to handle values with embedded semicolons or commas
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == ',') {
                String segment = header.substring(start, i).trim();
                parseInternal(segment, consumer);
                start = i + 1;
            }
        }

        if (start < len) {
            String segment = header.substring(start).trim();
            parseInternal(segment, consumer);
        }
    }

    /**
     * Parses cookies from the given header segment and processes them using the provided consumer.
     *
     * @param segment  the header segment to parse
     * @param consumer the consumer to process each parsed cookie
     */
    private static void parseInternal(String segment, BiConsumer<String, String> consumer) {
        int eqIndex = segment.indexOf('=');
        if (eqIndex != -1) {
            String name = segment.substring(0, eqIndex).trim();
            int endIndex = segment.indexOf(';', eqIndex);
            String value;
            if (endIndex != -1) {
                value = segment.substring(eqIndex + 1, endIndex).trim();
            } else {
                value = segment.substring(eqIndex + 1).trim();
            }
            if (!name.isEmpty()) {
                consumer.accept(name, stripOffSurroundingQuote(value));
            }
        }
    }

    /**
     * Strips off surrounding quotes from the given string if present.
     *
     * @param str the string from which to strip quotes
     * @return the string without surrounding quotes
     */
    private static String stripOffSurroundingQuote(String str) {
        if (str != null) {
            int length = str.length();
            if (length > 1) {
                char first = str.charAt(0);
                char last = str.charAt(length - 1);
                if (first == '"' && last == '"') {
                    return str.substring(1, length - 1);
                } else if (first == '\'' && last == '\'') {
                    return str.substring(1, length - 1);
                }
            }
        }
        return str;
    }
}
