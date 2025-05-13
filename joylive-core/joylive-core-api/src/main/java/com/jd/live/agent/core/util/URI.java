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
package com.jd.live.agent.core.util;

import lombok.Builder;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Represents a URI (Uniform Resource Identifier) with components like schema, host, port, path, and parameters.
 * Provides methods to construct and modify the URI.
 */
public class URI {

    @Getter
    private String schema;

    @Getter
    private String user;

    @Getter
    private String password;

    @Getter
    private String host;

    @Getter
    private Integer port;

    @Getter
    private String path;

    private Map<String, String> parameters;

    private String url;

    private Long id;

    /**
     * Default constructor.
     */
    public URI() {
    }

    private URI(String schema,
                String user,
                String password,
                String host,
                Integer port,
                String path,
                Map<String, String> parameters,
                String url) {
        this.schema = schema;
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters;
        this.url = url;
    }

    /**
     * Constructs a URI with the specified schema, host, path, and parameters.
     *
     * @param schema     the URI schema (e.g., "http", "https").
     * @param host       the host name or IP address.
     * @param port       the port.
     * @param path       the path component of the URI.
     * @param parameters the query parameters as a map.
     */
    public URI(String schema,
               String host,
               Integer port,
               String path,
               Map<String, String> parameters) {
        this(schema, null, null, host, port, path, parameters, null);
    }

    /**
     * Constructs a URI with the specified schema, host, path, and parameters.
     *
     * @param schema     the URI schema (e.g., "http", "https").
     * @param user       the user info.
     * @param password   the password info.
     * @param host       the host name or IP address.
     * @param port       the port.
     * @param path       the path component of the URI.
     * @param parameters the query parameters as a map.
     */
    @Builder
    public URI(String schema,
               String user,
               String password,
               String host,
               Integer port,
               String path,
               Map<String, String> parameters) {
        this(schema, user, password, host, port, path, parameters, null);
    }

    /**
     * Sets the schema component of the URI.
     *
     * @param schema the new schema.
     * @return a new URI instance with the updated schema.
     */
    public URI schema(String schema) {
        return new URI(schema, user, password, host, port, path, parameters);
    }

    /**
     * Sets the user component of the URI.
     *
     * @param user the new user name
     * @return a new URI instance with the updated user component
     */
    public URI user(String user) {
        return new URI(schema, user, password, host, port, path, parameters);
    }

    /**
     * Sets the password component of the URI.
     *
     * @param password the new password
     * @return a new URI instance with the updated password component
     */
    public URI password(String password) {
        return new URI(schema, user, password, host, port, path, parameters);
    }

    /**
     * Sets the host component of the URI.
     *
     * @param host the new host name or IP address.
     * @return a new URI instance with the updated host.
     */
    public URI host(String host) {
        return new URI(schema, user, password, host, port, path, parameters);
    }

    /**
     * Sets the port component of the URI.
     *
     * @param port the new port number.
     * @return a new URI instance with the updated port.
     */
    public URI port(Integer port) {
        return new URI(schema, user, password, host, port, path, parameters);
    }

    /**
     * Sets the path component of the URI.
     *
     * @param path the new path.
     * @return a new URI instance with the updated path.
     */
    public URI path(String path) {
        return new URI(schema, user, password, host, port, path, parameters);
    }

    /**
     * Sets the query component of the URI.
     *
     * @param parameters the new parameters.
     * @return a new URI instance with the updated path.
     */
    public URI parameters(Map<String, String> parameters) {
        return new URI(schema, user, password, host, port, path, parameters);
    }

    /**
     * Adds or updates a single query parameter.
     *
     * @param key   the parameter key.
     * @param value the parameter value.
     * @return a new URI instance with the updated parameters.
     */
    public URI parameter(String key, String value) {
        int keyLen = key == null ? 0 : key.length();
        if (keyLen == 0) {
            return this;
        }
        int size = parameters == null ? 0 : parameters.size();
        boolean keyExists = size != 0 && parameters.containsKey(key);
        Map<String, String> newParameters = size == 0 ? new HashMap<>(size + 1) : new HashMap<>(parameters);
        newParameters.put(key, value);
        String newUrl = null;
        if (url != null && !keyExists) {
            newUrl = url;
            // improve performance
            int valueLen = value == null ? 0 : value.length();
            StringBuilder builder = new StringBuilder(newUrl.length() + keyLen + valueLen + 2)
                    .append(newUrl)
                    .append(size > 0 ? '&' : '?')
                    .append(key);
            if (valueLen > 0) {
                builder.append('=').append(value);
            }
            newUrl = builder.toString();
        }
        return new URI(schema, user, password, host, port, path, newParameters, newUrl);
    }

    /**
     * Returns the address (host:port) without schema prefix.
     */
    public String getAddress() {
        return getAddress(false);
    }

    /**
     * Returns the formatted address string.
     *
     * @param withSchema whether to include schema prefix (e.g. "http://")
     * @return host:port (with schema if requested)
     */
    public String getAddress(boolean withSchema) {
        if (!withSchema || schema == null) {
            if (port == null) {
                return host;
            } else {
                return host + ":" + port;
            }
        }
        StringBuilder sb = new StringBuilder(64);
        sb.append(schema).append("://");
        if (host != null) {
            sb.append(host);
        }
        if (port != null) {
            sb.append(":").append(port);
        }
        return sb.toString();
    }

    /**
     * Retrieves the value of a specific query parameter.
     *
     * @param key the parameter key.
     * @return the parameter value, or {@code null} if the parameter does not exist.
     */
    public String getParameter(String key) {
        return parameters == null || key == null ? null : parameters.get(key);
    }

    public boolean hasParameter(String key) {
        return parameters != null && key != null && parameters.containsKey(key);
    }

    public Map<String, String> getParameters() {
        return parameters == null ? null : Collections.unmodifiableMap(parameters);
    }

    /**
     * Constructs the full URI string.
     *
     * @return the full URI string.
     */
    public String getUri() {
        if (url == null) {
            StringBuilder sb = new StringBuilder(128);
            if (schema != null && !schema.isEmpty()) {
                sb.append(schema).append("://");
            }
            int userLen = user == null ? 0 : user.length();
            int passwordLen = password == null ? 0 : password.length();
            if (userLen > 0) {
                sb.append(user);
            }
            if (passwordLen > 0) {
                sb.append(':').append(password);
            }
            if (userLen > 0 || passwordLen > 0) {
                sb.append('@');
            }
            if (host != null) {
                sb.append(host);
            }
            if (port != null) {
                sb.append(':').append(port);
            }
            if (path != null && !path.isEmpty()) {
                if (path.startsWith("/")) {
                    sb.append(path);
                } else {
                    sb.append('/').append(path);
                }
            }
            if (parameters != null && !parameters.isEmpty()) {
                sb.append('?');
                int count = 0;
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    if (count++ > 0) {
                        sb.append('&');
                    }
                    sb.append(entry.getKey());
                    if (entry.getValue() != null) {
                        sb.append("=").append(entry.getValue());
                    }
                }
            }
            url = sb.toString();
        }
        return url;
    }

    /**
     * Computes and retrieves a unique identifier for the URI based on its content.
     *
     * @return the unique identifier for the URI.
     */
    public Long getId() {
        if (id == null) {
            CRC32 crc32 = new CRC32();
            byte[] bytes = getUri().getBytes(StandardCharsets.UTF_8);
            crc32.update(bytes, 0, bytes.length);
            id = Math.abs(crc32.getValue());
        }
        return id;
    }

    @Override
    public String toString() {
        return getUri();
    }

    /**
     * Parses a given URI string into a {@link URI} object. This method processes the input string to extract
     * the protocol, host, port, path, and query parameters.
     *
     * @param uri The URI string to be parsed.
     * @return A {@link URI} object representing the parsed components of the input string, or {@code null} if the input is {@code null} or empty.
     */
    public static URI parse(String uri) {
        uri = uri == null ? null : uri.trim();
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        char[] chars = uri.toCharArray();
        int[] pos = URIPart.parse(chars, false);
        String schema = Role.SCHEMA.getPart(chars, pos);
        String user = Role.USER.getPart(chars, pos);
        String password = Role.PASSWORD.getPart(chars, pos);
        String host = Role.HOST.getPart(chars, pos);
        String port = Role.PORT.getPart(chars, pos);
        String path = Role.PATH.getPart(chars, pos);
        Map<String, String> parameters = URIPart.parseQuery(chars, pos);
        Integer p = null;
        if (port != null && !port.isEmpty()) {
            try {
                p = Integer.parseInt(port);
            } catch (NumberFormatException ignore) {
            }
        }
        return new URI(schema, user, password, host, p, path, parameters);
    }


    /**
     * Parses the host from a given URI.
     *
     * @param uri the URI to parse
     * @return the host part of the URI, or null if the URI is invalid or does not contain a host
     */
    public static String parseHost(String uri) {
        uri = uri == null ? null : uri.trim();
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        char[] chars = uri.toCharArray();
        int[] pos = URIPart.parse(chars, true);
        return Role.HOST.getPart(chars, pos);
    }

    /**
     * Represents a parsed URI with all components.
     * Handles schema, authentication, host, port, path, query and fragment.
     */
    private static class URIPart {

        /**
         * Parses URI components from string into position markers array.
         * The returned array contains start/end position pairs for each URI component.
         *
         * @param uri      the URI string to parse (non-null)
         * @param onlyHost if true, only parses host-related components (scheme, user, password, host, port)
         * @return array of 16 integers representing component positions (even indices for start,
         * odd for end positions, -1 means undefined)
         * @throws NullPointerException if uri is null
         */
        public static int[] parse(char[] uri, boolean onlyHost) {
            int[] pos = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
            doParse(uri, onlyHost, pos);
            return pos;
        }

        /**
         * Parses query parameters from URI into key-value pairs.
         * Handles standard URL-encoded query format (key1=value1&key2=value2).
         * Empty values are converted to null, empty keys are ignored.
         *
         * @param uri the URI containing the query string (non-null)
         * @param pos position array from parse() method
         * @return Map of query parameters (key → value), or null if no query present
         * @throws NullPointerException     if uri or pos is null
         * @throws IllegalArgumentException if pos array is malformed
         */
        public static Map<String, String> parseQuery(char[] uri, int[] pos) {
            if (Role.QUERY.isBeginless(pos)) {
                return null;
            }
            Map<String, String> map = new HashMap<>(4);
            int keyStart = Role.QUERY.getStart(pos);
            int keyEnd = -1;
            int valueStart = -1;
            int valueEnd = -1;
            int length = uri.length;
            char c;
            for (int i = keyStart; i < length; i++) {
                c = uri[i];
                switch (c) {
                    case '=':
                        keyEnd = i;
                        valueStart = i + 1;
                        break;
                    case '&':
                        // another parameter
                        addParameter(map, uri, i, keyStart, keyEnd, valueStart, valueEnd);
                        keyStart = i + 1;
                        keyEnd = -1;
                        valueStart = -1;
                        valueEnd = -1;
                        break;
                }
            }
            addParameter(map, uri, length, keyStart, keyEnd, valueStart, valueEnd);
            return map;
        }

        /**
         * Helper method to add a single parameter to the map.
         *
         * @param map        the target parameter map
         * @param uri        the source URI string
         * @param pos        current parsing position
         * @param keyStart   start index of parameter key
         * @param keyEnd     end index of parameter key (-1 if no '=' found)
         * @param valueStart start index of parameter value
         * @param valueEnd   end index of parameter value
         */
        private static void addParameter(Map<String, String> map, char[] uri,
                                         int pos, int keyStart, int keyEnd, int valueStart, int valueEnd) {
            String key;
            String value;
            if (keyEnd == -1) {
                keyEnd = pos;
            } else {
                valueEnd = pos;
            }
            if (keyStart != -1) {
                key = new String(uri, keyStart, keyEnd - keyStart);
                value = valueStart == -1 ? null : new String(uri, valueStart, valueEnd - valueStart);
                value = value == null || value.isEmpty() ? null : value;
                if (!key.isEmpty()) {
                    map.put(key, value);
                }
            }
        }

        /**
         * Parses URI components from string.
         *
         * @param uri      the URI string to parse
         * @param onlyHost if true, only parses host-related components
         */
        private static void doParse(char[] uri, boolean onlyHost, int[] pos) {
            boolean success;
            if (onlyHost) {
                doParseHost(uri, pos);
                if (Role.HOST.isEndless(pos)) {
                    success = !Role.PATH.align(pos, Role.HOST) && Role.QUERY.align(pos, Role.HOST);
                }
                return;
            }
            doParseHost(uri, pos);
            doParsePath(uri, pos);
            if (Role.PATH.isEndless(pos)) {
                success = Role.QUERY.align(pos, Role.PATH);
            }
            if (Role.PORT.isEndless(pos)) {
                success = !Role.PATH.align(pos, Role.PORT) && Role.QUERY.align(pos, Role.PORT);
            }
            if (Role.HOST.isEndless(pos)) {
                success = !Role.PATH.align(pos, Role.HOST) && Role.QUERY.align(pos, Role.HOST);
            }
            if (Role.PASSWORD.isEndless(pos)) {
                success = !Role.HOST.align(pos, Role.PASSWORD) && !Role.PATH.align(pos, Role.PASSWORD) && Role.QUERY.align(pos, Role.PASSWORD);
            }
            if (Role.USER.isEndless(pos)) {
                success = !Role.PASSWORD.align(pos, Role.USER) && !Role.HOST.align(pos, Role.USER) && !Role.PATH.align(pos, Role.USER) && Role.QUERY.align(pos, Role.USER);
            }
        }

        /**
         * Extracts path component from URI.
         * Stops at query ('?'), parameter ('=') or fragment ('#').
         */
        private static void doParsePath(char[] uri, int[] pos) {
            if (Role.PATH.isBeginless(pos)) {
                return;
            }
            int length = uri.length;
            for (int i = Role.PATH.getStart(pos); i < length; i++) {
                switch (uri[i]) {
                    case '?':
                        // query
                        Role.PATH.setEnd(pos, i);
                        Role.QUERY.setStart(pos, i + 1);
                        return;
                    case '=':
                        Role.PATH.setEnd(pos, i);
                        Role.QUERY.setStart(pos, i);
                        return;
                }
            }
        }


        /**
         * Extracts host-related components (schema, auth, host, port).
         * Supports IPv6 addresses in brackets and authentication.
         */
        private static void doParseHost(char[] uri, int[] pos) {
            boolean schemaFlag = false;
            int lastColon = -1;
            int length = uri.length;
            Role.HOST.setStart(pos, 0);
            for (int i = 0; i < length; i++) {
                switch (uri[i]) {
                    case '[':
                        // http://[1080:0:0:0:8:800:200C:417A]/index.html
                        Role.IPV6.setStart(pos, i + 1);
                        Role.HOST.setStart(pos, i + 1);
                        Role.PORT.setStart(pos, -1);
                        break;
                    case ']':
                        if (Role.IPV6.getStart(pos) >= 0) {
                            Role.IPV6.setEnd(pos, i);
                            Role.HOST.setEnd(pos, i);
                            Role.PORT.setStart(pos, -1);
                        }
                        break;
                    case '/':
                        Role.PATH.setStart(pos, i + 1);
                        return;
                    case '?':
                        Role.QUERY.setStart(pos, i + 1);
                        return;
                    case '=':
                        Role.QUERY.setStart(pos, i);
                        return;
                    case '@':
                        // user:password@host:port
                        int schemaEnd = Role.SCHEMA.getEnd(pos);
                        if (lastColon >= 0) {
                            Role.USER.setPosition(pos, schemaEnd == -1 ? 0 : schemaEnd + 3, lastColon);
                            Role.PASSWORD.setPosition(pos, lastColon + 1, i);
                            Role.HOST.setEnd(pos, -1);
                            Role.PORT.setStart(pos, -1);
                            lastColon = -1;
                        } else {
                            Role.USER.setPosition(pos, schemaEnd == -1 ? 0 : schemaEnd + 3, i);
                        }
                        Role.HOST.setStart(pos, i + 1);
                        break;
                    case ':':
                        if ((Role.IPV6.isBeginless(pos) || !Role.IPV6.isEndless(pos)) && Role.PORT.isBeginless(pos)) {
                            if (!schemaFlag) {
                                schemaFlag = true;
                                if ((length - i >= 3) && uri[i + 1] == '/' && uri[i + 2] == '/') {
                                    Role.SCHEMA.setPosition(pos, 0, i);
                                    Role.HOST.setStart(pos, i + 3);
                                    i += 2;
                                    break;
                                }
                            }
                            if (Role.HOST.isEndless(pos)) {
                                Role.HOST.setEnd(pos, i);
                            }
                            Role.PORT.setStart(pos, i + 1);
                        }
                        lastColon = i;
                        break;
                }
            }
        }

    }

    /**
     * Represents portions of a URI with their start and end positions.
     * Provides methods to extract, align and validate URI components.
     */
    @Getter
    private enum Role {
        /**
         * URI scheme component (e.g., "http")
         */
        SCHEMA(0, 1),

        /**
         * User authentication component
         */
        USER(2, 3),

        /**
         * Password authentication component
         */
        PASSWORD(4, 5),

        /**
         * Host component with IPv6 handling
         */
        HOST(6, 7) {
            @Override
            public String getPart(char[] uri, int[] pos, boolean emptyAsNull) {
                if (!IPV6.isBeginless(pos) && IPV6.isEndless(pos)) {
                    return null;
                }
                return super.getPart(uri, pos, emptyAsNull);
            }
        },

        /**
         * IPv6 address component
         */
        IPV6(8, 9),

        /**
         * Port number component
         */
        PORT(10, 11),

        /**
         * Path component
         */
        PATH(12, 13) {
            @Override
            protected String getPart(char[] uri, int[] pos, int start, int end, boolean emptyAsNull) {
                return super.getPart(uri, pos, start > 0 ? start - 1 : start, end, emptyAsNull);
            }
        },

        /**
         * Query string component
         */
        QUERY(14, 15);

        protected final int startPos;
        protected final int endPos;

        Role(int startPos, int endPos) {
            this.startPos = startPos;
            this.endPos = endPos;
        }

        /**
         * Aligns this portion with previous portion
         *
         * @param pos      Position array
         * @param previous Previous portion
         * @return true if alignment successful
         */
        public boolean align(int[] pos, Role previous) {
            int thisStart = pos[startPos];
            int previousStart = pos[previous.startPos];
            if (thisStart >= 0) {
                if (thisStart == previousStart) {
                    pos[previous.startPos] = -1;
                }
                pos[previous.endPos] = thisStart - 1;
                return true;
            }
            return false;
        }

        /**
         * Checks if this portion has no defined end
         *
         * @param pos Position array
         */
        public boolean isEndless(int[] pos) {
            return isEndless(pos, startPos, endPos);
        }

        /**
         * Checks if this portion has no defined start
         *
         * @param pos Position array
         */
        public boolean isBeginless(int[] pos) {
            return isBeginless(pos, startPos);
        }

        /**
         * Extracts this portion from URI with empty-as-null handling
         *
         * @param uri URI string
         * @param pos Position array
         */
        public String getPart(char[] uri, int[] pos) {
            return getPart(uri, pos, pos[startPos], pos[endPos], true);
        }

        /**
         * Extracts this portion from URI
         *
         * @param uri         URI string
         * @param pos         Position array
         * @param emptyAsNull Convert empty strings to null
         */
        public String getPart(char[] uri, int[] pos, boolean emptyAsNull) {
            return getPart(uri, pos, pos[startPos], pos[endPos], emptyAsNull);
        }

        /**
         * Gets the start position from the position array
         *
         * @param pos The array containing position markers
         * @return The start position value, or -1 if not set
         */
        public int getStart(int[] pos) {
            return pos[startPos];
        }

        /**
         * Sets the start position in the position array
         *
         * @param pos   The array containing position markers
         * @param value The value to set as start position
         */
        public void setStart(int[] pos, int value) {
            pos[startPos] = value;
        }

        /**
         * Gets the end position from the position array
         *
         * @param pos The array containing position markers
         * @return The end position value, or -1 if not set
         */
        public int getEnd(int[] pos) {
            return pos[endPos];
        }

        /**
         * Sets the end position in the position array
         *
         * @param pos   The array containing position markers
         * @param value The value to set as end position
         */
        public void setEnd(int[] pos, int value) {
            pos[endPos] = value;
        }

        /**
         * Sets both start and end positions simultaneously
         *
         * @param pos   The array containing position markers
         * @param start The start position value
         * @param end   The end position value
         */
        public void setPosition(int[] pos, int start, int end) {
            pos[startPos] = start;
            pos[endPos] = end;
        }

        /**
         * Extracts a substring from URI chars using position markers.
         *
         * @param uri         Char array containing URI
         * @param pos         Array with start/end positions
         * @param start       Index in pos array for start position
         * @param end         Index in pos array for end position
         * @param emptyAsNull If true, returns null for empty strings
         * @return Extracted string, null if invalid positions, or empty string (when emptyAsNull is true)
         */
        protected String getPart(char[] uri, int[] pos, int start, int end, boolean emptyAsNull) {
            String result = null;
            int length = uri.length;
            if (start >= 0 && start < length) {
                if (end == -1) {
                    result = new String(uri, start, length - start);
                } else {
                    result = new String(uri, start, end - start);
                }
            }
            if (result == null) {
                return result;
            }
            return emptyAsNull && result.isEmpty() ? null : result;
        }

        /**
         * Checks if a position is undefined (has no beginning)
         *
         * @param pos      The array containing position markers
         * @param startPos The index of the start position
         * @return true if the position is undefined (-1)
         */
        public static boolean isBeginless(int[] pos, int startPos) {
            return pos[startPos] == -1;
        }

        /**
         * Checks if a position has no defined end (but has a start)
         *
         * @param pos      The array containing position markers
         * @param startPos The index of the start position
         * @param endPos   The index of the end position
         * @return true if start is defined (≥0) but end is undefined (-1)
         */
        public static boolean isEndless(int[] pos, int startPos, int endPos) {
            return pos[startPos] >= 0 && pos[endPos] == -1;
        }
    }

}
