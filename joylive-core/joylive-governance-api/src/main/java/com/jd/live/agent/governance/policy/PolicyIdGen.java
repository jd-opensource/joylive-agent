package com.jd.live.agent.governance.policy;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@code PolicyIdGen} interface defines a contract for generating unique identifiers that can be
 * used to supplement information with an ID. It provides a method for generating an ID and associating
 * it with a given URL and set of tags.
 *
 * @see Supplier
 * @see Map
 */
public interface PolicyIdGen {

    /**
     * Appends a query parameter to a given URL.
     *
     * <p>
     * This method takes a base URL and appends a single query parameter to it.
     * If the URL already contains query parameters, the new parameter is appended with an ampersand ('&').
     * If it does not, the parameter is appended with a question mark ('?') to start the query string.
     * </p>
     *
     * @param url   the base URL to which the query parameter will be appended.
     * @param query the query parameter key.
     * @param value the value associated with the query parameter.
     * @return a new URL with the query parameter appended.
     */
    default String addQuery(String url, String query, String value) {
        return url + (url.contains("?") ? "&" : "?") + query + "=" + value;
    }

    /**
     * Appends a path to a given URL.
     *
     * <p>
     * This method takes a base URL and a path, and appends the path to the URL.
     * It handles the leading slash of the path depending on whether the URL already ends with a slash.
     * </p>
     *
     * @param url  the base URL to which the path will be appended.
     * @param path the path to be appended to the base URL.
     * @return a new URL with the path appended.
     */
    default String addPath(String url, String path) {
        boolean ends = url != null && url.endsWith("/");
        boolean starts = path != null && path.startsWith("/");
        if (ends) {
            return url + (!starts ? path : path.substring(1));
        }
        return url + (starts ? path : ("/" + path));
    }


    /**
     * Generates a unique identifier with the provided URL, along with any
     * specified tags. The URL is obtained from the given supplier, which is expected to return a
     * non-null URL string. The tags are a key-value pair mapping that provides additional context
     * or metadata for the ID being generated.
     *
     * @param urlSupplier a {@code Supplier<String>} that provides the URL.
     * @param tags        a {@code Map<String, String>} containing the tags to be associated with the generated ID.
     * @throws IllegalArgumentException if the provided URL from the supplier is null or empty.
     */
    void supplement(Supplier<String> urlSupplier, Map<String, String> tags);

}