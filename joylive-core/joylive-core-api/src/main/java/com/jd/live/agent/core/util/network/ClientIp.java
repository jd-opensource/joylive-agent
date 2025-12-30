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
package com.jd.live.agent.core.util.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ClientIp {

    private static final String[] CUSTOM_CLIENT_IP_HEADERS = getCustomIpHeaders();

    /**
     * Extracts client IP address from headers using header function.
     *
     * @param headerFunc function to retrieve header value by name
     * @return client IP address or null if not found
     */
    public static String getIp(Function<String, String> headerFunc) {
        return getIp(headerFunc, null);
    }

    /**
     * Extracts client IP address from headers using function.
     *
     * @param headerFunc      function to retrieve header value by name
     * @param defaultSupplier function to retrieve default value if not found
     * @return client IP address or null if not found
     */
    public static String getIp(Function<String, String> headerFunc, Supplier<String> defaultSupplier) {
        String ip = getIpByRfc7239(headerFunc);
        if (ip != null) {
            return ip;
        }
        ip = getIpByCustomHeaders(headerFunc);
        if (ip != null) {
            return ip;
        }
        return defaultSupplier == null ? null : defaultSupplier.get();
    }

    /**
     * Extracts client IP address from custom headers.
     *
     * @param headerFunc function to retrieve header value by name
     * @return client IP address or null if not found
     */
    private static String getIpByCustomHeaders(Function<String, String> headerFunc) {
        String forwards;
        for (String header : CUSTOM_CLIENT_IP_HEADERS) {
            forwards = headerFunc.apply(header);
            if (forwards != null && !forwards.isEmpty()) {
                // multiple forward ips, such as X-Forwarded-For: 192.168.1.1, 192.168.1.2
                int pos = forwards.indexOf(',');
                if (pos > 0) {
                    return forwards.substring(0, pos);
                }
                return forwards;
            }
        }
        return null;
    }

    /**
     * Extracts client IP address from RFC 7239 Forwarded header.
     *
     * @param headerFunc function to retrieve header value by name
     * @return client IP address or null if not found
     */
    private static String getIpByRfc7239(Function<String, String> headerFunc) {
        String forwarded = headerFunc.apply("Forwarded");
        if (forwarded == null || forwarded.isEmpty()) {
            return null;
        }
        forwarded = forwarded.trim();
        // Forwarded: for=192.0.2.43, for="192.0.2.45:9999", for="[2001:db8:cafe::17]:47011"
        int pos = forwarded.indexOf(',');
        if (pos > 0) {
            // first part is the client real ip
            forwarded = forwarded.substring(0, pos).trim();
        }
        // Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
        int start = 0;
        pos = forwarded.indexOf(';', start);
        String part;
        while (pos != -1) {
            part = forwarded.substring(start, pos);
            if (part.startsWith("for=")) {
                return getIpByFor(part);
            }
            start = pos + 1;
            pos = forwarded.indexOf(';', start);
        }
        if (start < forwarded.length()) {
            part = forwarded.substring(start);
            if (part.startsWith("for=")) {
                return getIpByFor(part);
            }
        }
        return null;
    }

    /**
     * Extracts client IP address from RFC 7239 Forwarded header string.
     *
     * @param value the Forwarded header value part (e.g., "for=192.0.2.60" or "for=\"192.0.2.45:9999\"")
     * @return the extracted client IP address, or empty string if invalid format, or null for IPv6 parsing errors
     */
    private static String getIpByFor(String value) {
        String ip = value.substring(4);
        if (ip.isEmpty()) {
            // invalid format, ignore
            return "";
        } else if (!ip.startsWith("\"")) {
            // for=192.0.2.60
            return ip;
        } else if (!ip.endsWith("\"")) {
            // invalid format, ignore
            return "";
        } else {
            ip = ip.substring(1, ip.length() - 1);
            if (ip.startsWith("[")) {
                // for="[2001:db8:cafe::17]:47011"
                int pos = ip.lastIndexOf(']');
                return pos == -1 ? null : ip.substring(0, pos + 1);
            } else {
                // for="192.0.2.45:9999"
                int pos = ip.lastIndexOf(':');
                if (pos == -1) {
                    return ip;
                } else if (pos == 0) {
                    // invalid format, ignore
                    return null;
                }
                return ip.substring(0, pos);
            }
        }
    }

    /**
     * Get client ip headers
     *
     * @return client ip headers
     */
    private static String[] getCustomIpHeaders() {
        String headers = System.getenv("CONFIG_CLIENT_IP_HEADERS");
        List<String> result = new ArrayList<>();
        if (headers != null && !headers.isEmpty()) {
            String[] parts = headers.split("[;,]");
            for (String part : parts) {
                part = part.trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
            }
        }
        if (result.isEmpty()) {
            return new String[]{"X-Forwarded-For"};
        }
        return result.toArray(new String[0]);
    }
}
