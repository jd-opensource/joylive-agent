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
package com.jd.live.agent.governance.util;

import java.net.HttpCookie;
import java.util.*;

/**
 * Provides utility methods for parsing cookies from HTTP request headers. This class is designed
 * to simplify the extraction and handling of cookies from the "Cookie" headers in HTTP requests.
 * Cookies are parsed and organized into a map that groups cookie values by their names, facilitating
 * easy access and manipulation of cookie data.
 *
 * <p>This class is abstract and contains static methods, making it a utility class that is not meant
 * to be instantiated. Instead, its public static methods are directly accessible for processing cookies.</p>
 */
public abstract class Cookies {

    /**
     * Parses cookies from the "Cookie" headers of the request.
     *
     * @param headers the collection of "Cookie" headers
     * @return a map of cookie names to lists of cookie values
     */
    public static Map<String, List<String>> parse(Collection<String> headers) {
        Map<String, List<String>> result = new HashMap<>();
        if (headers == null || headers.isEmpty()) {
            return result;
        }
        for (String header : headers) {
            if (header != null && !header.isEmpty()) {
                String[] values = header.split(";");
                List<HttpCookie> cookies;
                for (String cookie : values) {
                    cookies = HttpCookie.parse(cookie.trim());
                    cookies.forEach(c -> result.computeIfAbsent(c.getName(), k -> new ArrayList<>()).add(c.getValue()));
                }
            }
        }
        return result;
    }
}
