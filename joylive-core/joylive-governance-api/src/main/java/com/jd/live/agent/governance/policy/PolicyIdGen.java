/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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