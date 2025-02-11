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
package com.jd.live.agent.core.util.http;

import com.jd.live.agent.core.parser.ObjectReader;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.map.CaseInsensitiveLinkedMap;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.core.util.map.MultiMap;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Utility class for handling HTTP requests with support for gzip and deflate compression.
 * This class provides a static method to perform GET requests and process the response.
 *
 * @author The author
 */
public abstract class HttpUtils {

    public static final String ENCODING_GZIP = "gzip";

    public static final String ENCODING_DEFLATE = "deflate";

    public static final String CONNECTION_KEEP_ALIVE = "keep-alive";

    public static final String ACCEPT_ENCODING_GZIP_DEFLATE = ENCODING_GZIP + "," + ENCODING_DEFLATE;

    private static final LazyObject<URIConstructor> cache = new LazyObject<>(() -> {
        try {
            return new URIConstructor();
        } catch (Throwable e) {
            return null;
        }
    });

    /**
     * Performs an HTTP GET request to the specified URI, configures the connection,
     * reads the response using a provided reader, and returns an HttpResponse object.
     *
     * @param uri        The URI to send the GET request to.
     * @param configure  A Consumer that accepts an HttpURLConnection which can be used to set up headers,
     *                   timeouts, or other connection configurations.
     * @param reader     An HttpReader functional interface that defines how to read the response body
     *                   from a Reader and convert it into the expected type T.
     * @param <T>        The type of the body expected in the HttpResponse.
     * @return An HttpResponse object containing the status code and the body read from
     *                   the response.
     * @throws IOException If an I/O error occurs while creating the URL object, opening the connection,
     *                     reading the response, or if the URL is not valid.
     */
    public static <T> HttpResponse<T> get(String uri, Consumer<HttpURLConnection> configure, ObjectReader<Reader, T> reader) throws IOException {
        if (!uri.contains("://")) {
            uri = "http://" + uri;
        }
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            // Set up the connection properties
            connection.setRequestProperty(HttpHeader.CONNECTION, CONNECTION_KEEP_ALIVE);
            connection.setRequestProperty(HttpHeader.ACCEPT_ENCODING, ACCEPT_ENCODING_GZIP_DEFLATE);
            connection.setRequestProperty(HttpHeader.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
            connection.setRequestMethod(HttpMethod.GET.name());
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(true);
            // Apply additional configuration
            configure.accept(connection);
            // Establish the connection
            connection.connect();
            // Get the response code
            HttpStatus status = HttpStatus.resolve(connection.getResponseCode());
            // Return the appropriate HttpResponse based on the status code
            if (status != null) {
                switch (status) {
                    case OK:
                        return new HttpResponse<>(status, getResponse(connection, reader));
                    case NOT_MODIFIED:
                    case NOT_FOUND:
                        return new HttpResponse<>(status, null);
                    default:
                        return status.isError() ? new HttpResponse<>(status, getErrorMessage(connection)) : new HttpResponse<>(status, null);
                }
            }
            return new HttpResponse<>(status, getErrorMessage(connection));

        } finally {
            // Disconnect the connection
            connection.disconnect();
        }
    }

    /**
     * Reads the response from the given HttpURLConnection and processes it using the provided reader.
     *
     * @param connection The HttpURLConnection from which to read the response.
     * @param reader     The reader that defines how to process the response body.
     * @param <T>        The type of the processed response body.
     * @return The processed response body.
     * @throws IOException If an I/O error occurs while reading the response.
     */
    private static <T> T getResponse(HttpURLConnection connection, ObjectReader<Reader, T> reader) throws IOException {
        return read(connection.getInputStream(), connection.getContentEncoding(), reader::read);
    }

    /**
     * Reads the error message from the given HttpURLConnection.
     *
     * @param connection The HttpURLConnection from which to read the error message.
     * @return A string containing the error message.
     * @throws IOException If an I/O error occurs while reading the error message.
     */
    private static String getErrorMessage(HttpURLConnection connection) throws IOException {
        return read(connection.getErrorStream(), connection.getContentEncoding(), reader -> {
            StringBuilder builder = new StringBuilder();
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                builder.append(inputLine);
            }
            return builder.toString();
        });
    }

    /**
     * Reads from the given InputStream, decompresses it if necessary, and uses the provided HttpReader
     * to process the input.
     *
     * @param stream   The InputStream to read from.
     * @param encoding The encoding used for the InputStream, which determines whether decompression is needed.
     * @param reader   The HttpReader that defines how to process the BufferedReader.
     * @param <T>      The type of the processed data.
     * @return The processed data.
     * @throws IOException If an I/O error occurs while reading from the InputStream.
     */
    private static <T> T read(InputStream stream, String encoding, ObjectReader<BufferedReader, T> reader) throws IOException {
        if (stream == null) {
            return null;
        }
        InputStream is = stream;
        if (encoding != null && encoding.contains(ENCODING_GZIP)) {
            is = new GZIPInputStream(is);
        } else if (encoding != null && encoding.contains(ENCODING_DEFLATE)) {
            is = new InflaterInputStream(is, new Inflater(true));
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.read(br);
        }
    }

    /**
     * Parses a query string and applies the given consumer to each key-value pair.
     *
     * @param query    the query string to parse
     * @param decode   whether the query string is URL decode
     * @param consumer a consumer that accepts each key-value pair
     */
    public static void parseQuery(String query, boolean decode, BiConsumer<String, String> consumer) {
        if (query == null || query.isEmpty()) {
            return;
        }

        int length = query.length();
        int start = 0;
        int end;
        int equalIndex;
        String key;
        String value = null;
        while (start < length) {
            end = query.indexOf('&', start);
            if (end == -1) {
                end = length;
            }
            equalIndex = query.indexOf('=', start);
            if (equalIndex == -1) {
                key = query.substring(start, end).trim();
                key = decode ? decodeURL(key) : key;
                value = null;
            } else if (equalIndex < end) {
                key = query.substring(start, equalIndex).trim();
                key = decode ? decodeURL(key) : key;
                value = query.substring(equalIndex + 1, end).trim();
                value = decode ? decodeURL(value) : value;
            } else {
                key = null;
            }
            if (key != null && !key.isEmpty()) {
                consumer.accept(key, value);
            }
            start = end + 1;
        }
    }

    /**
     * Parses a query string and returns a map of key-value pairs.
     *
     * @param query the query string to parse
     * @return a map where each key is associated with a list of values
     */
    public static MultiMap<String, String> parseQuery(String query) {
        return parseQuery(query, true);
    }

    /**
     * Parses a query string and returns a map where each key is associated with a list of values.
     *
     * @param query  the query string to parse
     * @param decode whether the query string is URL decoded
     * @return a map where each key is associated with a list of values
     */
    public static MultiMap<String, String> parseQuery(String query, boolean decode) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        MultiMap<String, String> result = new MultiLinkedMap<>();
        parseQuery(query, decode, (key, value) -> result.add(key, value == null ? "" : value));
        return result;
    }

    /**
     * Parses an array of cookie objects into a MultiMap where the keys are the cookie names and the values are the cookie values.
     *
     * @param cookies   the array of cookie objects
     * @param keyFunc   a function that extracts the cookie name from a cookie object
     * @param valueFunc a function that extracts the cookie value from a cookie object
     * @param <T>       the type of the cookie objects
     * @return a MultiMap containing the parsed cookies, or null if the input is invalid
     */
    public static <T> MultiMap<String, String> parseCookie(T[] cookies, Function<T, String> keyFunc, Function<T, String> valueFunc) {
        if (cookies == null || cookies.length == 0 || keyFunc == null || valueFunc == null) {
            return null;
        }
        MultiMap<String, String> result = new MultiLinkedMap<>(CaseInsensitiveLinkedMap::new);
        for (T cookie : cookies) {
            result.add(keyFunc.apply(cookie), valueFunc.apply(cookie));
        }
        return result;
    }

    /**
     * Parses cookies from the "Cookie" headers of the request.
     *
     * @param headers the collection of "Cookie" headers
     * @return a map of cookie names to lists of cookie values
     */
    public static MultiMap<String, String> parseCookie(Collection<String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        MultiMap<String, String> result = new MultiLinkedMap<>(CaseInsensitiveLinkedMap::new);
        for (String header : headers) {
            parseCookie(header, (key, value) -> result.add(key, value == null ? "" : value));
        }
        return result;
    }

    /**
     * Parses cookies from the "Cookie" header of the request.
     *
     * @param header cooke header string
     * @return a map of cookie names to lists of cookie values
     */
    public static MultiMap<String, String> parseCookie(String header) {
        if (header == null || header.isEmpty()) {
            return null;
        }
        MultiMap<String, String> result = new MultiLinkedMap<>(CaseInsensitiveLinkedMap::new);
        parseCookie(header, (key, value) -> result.add(key, value == null ? "" : value));
        return result;
    }

    /**
     * Parses a cookie string and applies the given consumer to each cookie name-value pair.
     *
     * @param value    the cookie string to parse
     * @param consumer a consumer that accepts each cookie name-value pair
     */
    public static void parseCookie(String value, BiConsumer<String, String> consumer) {
        Cookies.parse(value, consumer);
    }

    /**
     * Converts a map of cookies with generic type values to a map of cookies with string values.
     *
     * @param <T>       the type of the cookie value
     * @param cookies   a map where the key is the cookie name and the value is a list of cookie values of type T
     * @param valueFunc a function that converts a value of type T to a string
     * @return a map where the key is the cookie name and the value is a list of cookie values as strings
     */
    public static <T> MultiMap<String, String> parseCookie(Map<String, List<T>> cookies, Function<T, String> valueFunc) {
        if (cookies == null || cookies.isEmpty() || valueFunc == null) {
            return null;
        }
        MultiMap<String, String> result = new MultiLinkedMap<>(CaseInsensitiveLinkedMap::new);
        cookies.forEach((name, cooke) -> {
            if (!cooke.isEmpty()) {
                if (cooke.size() == 1) {
                    result.set(name, valueFunc.apply(cooke.get(0)));
                } else {
                    List<String> values = new ArrayList<>(cooke.size());
                    cooke.forEach(value -> values.add(valueFunc.apply(value)));
                    result.setAll(name, values);
                }
            }
        });
        return result;
    }

    /**
     * Parses HTTP headers from the given enumeration of header names and a function to retrieve header values.
     *
     * @param names      an enumeration of header names
     * @param headerFunc a function that takes a header name and returns an enumeration of its values
     * @return a map where each key is a header name and the value is a list of header values
     */
    public static MultiMap<String, String> parseHeader(Enumeration<String> names,
                                                       Function<String, Enumeration<String>> headerFunc) {
        if (names == null || headerFunc == null || !names.hasMoreElements()) {
            return null;
        }
        MultiMap<String, String> result = new MultiLinkedMap<>(CaseInsensitiveLinkedMap::new);
        String name;
        Enumeration<String> valueEnumeration;
        while (names.hasMoreElements()) {
            name = names.nextElement();
            valueEnumeration = headerFunc.apply(name);
            if (valueEnumeration != null) {
                while (valueEnumeration.hasMoreElements()) {
                    result.add(name, valueEnumeration.nextElement());
                }
            }
        }
        return result;
    }

    /**
     * Decodes a URL encoded string using UTF-8 encoding.
     * If the decoding fails due to an unsupported encoding exception, the original string is returned.
     *
     * @param s the URL encoded string to decode
     * @return the decoded string, or the original string if decoding fails
     */
    public static String decodeURL(String s) {
        try {
            return decodeURL(s, StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    /**
     * Decodes a URL encoded string using a specified encoding.
     * This method is optimized to minimize object creation and reuse the original string as much as possible.
     *
     * @param s       the URL encoded string to decode
     * @param charset the character encoding to use for decoding
     * @return the decoded string
     * @throws UnsupportedEncodingException if the specified encoding is not supported
     * @throws IllegalArgumentException     if the input string contains incomplete escape sequences
     */
    public static String decodeURL(String s, Charset charset) throws UnsupportedEncodingException {
        if (s == null) {
            return null;
        }

        int length = s.length();
        char[] result = new char[length];
        int resultPos = 0;
        byte[] bytes = new byte[length / 3];
        int bytesPos = 0;

        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == '+') {
                result[resultPos++] = ' ';
            } else if (c == '%') {
                bytesPos = 0;
                while (i + 2 < length && c == '%') {
                    int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                    bytes[bytesPos++] = (byte) v;
                    i += 3;
                    if (i < length) {
                        c = s.charAt(i);
                    }
                }
                if (i < length && c == '%') {
                    throw new UnsupportedEncodingException("Incomplete trailing escape (%) pattern");
                }
                String decodedChunk = new String(bytes, 0, bytesPos, charset);
                for (int j = 0; j < decodedChunk.length(); j++) {
                    result[resultPos++] = decodedChunk.charAt(j);
                }
                if (i < length) {
                    i--; // Adjust for the outer loop increment
                }
            } else {
                result[resultPos++] = c;
            }
        }

        return new String(result, 0, resultPos);
    }

    /**
     * Creates a new URI object with the specified components.
     *
     * @param uri    the original URI object
     * @param scheme the scheme component of the new URI
     * @param host   the host component of the new URI
     * @param port   the port component of the new URI
     * @return a new URI object with the specified components
     */
    public static URI newURI(URI uri, String scheme, String host, Integer port) {
        int p = port != null ? port : uri.getPort();
        String s = scheme == null || scheme.isEmpty() ? uri.getScheme() : scheme;
        String h = host == null || host.isEmpty() ? uri.getHost() : host;
        URIConstructor constructor = cache.get();
        if (constructor != null) {
            try {
                return constructor.create(s, uri.getRawUserInfo(), h, p, uri.getRawPath(), uri.getRawQuery(), uri.getRawFragment());
            } catch (Throwable ignored) {
            }
        }
        try {
            return new URI(s, uri.getRawUserInfo(), h, p, uri.getRawPath(), uri.getRawQuery(), uri.getRawFragment());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Creates a new URI object with the specified components.
     *
     * @param uri  the original URI object
     * @param host the host component of the new URI
     * @return a new URI object with the specified components
     */
    public static URI newURI(URI uri, String host) {
        return newURI(uri, null, host, null);
    }

    /**
     * A private static class that provides a way to construct URI objects using reflection.
     */
    private static class URIConstructor {

        private final Constructor<URI> constructor;

        private final Field schemeField;

        private final Field fragmentField;

        private final Field authorityField;

        private final Field userInfoField;

        private final Field hostField;

        private final Field portField;

        private final Field pathField;

        private final Field queryField;

        @SuppressWarnings("unchecked")
        URIConstructor() throws NoSuchFieldException, NoSuchMethodException {
            Class<?> uriClass = URI.class;
            constructor = (Constructor<URI>) uriClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            schemeField = uriClass.getDeclaredField("scheme");
            schemeField.setAccessible(true);
            fragmentField = uriClass.getDeclaredField("fragment");
            fragmentField.setAccessible(true);
            authorityField = uriClass.getDeclaredField("authority");
            authorityField.setAccessible(true);
            userInfoField = uriClass.getDeclaredField("userInfo");
            userInfoField.setAccessible(true);
            hostField = uriClass.getDeclaredField("host");
            hostField.setAccessible(true);
            portField = uriClass.getDeclaredField("port");
            portField.setAccessible(true);
            pathField = uriClass.getDeclaredField("path");
            pathField.setAccessible(true);
            queryField = uriClass.getDeclaredField("query");
            queryField.setAccessible(true);
        }

        /**
         * Creates a new URI object with the specified components.
         *
         * @param scheme   the scheme component of the new URI
         * @param userInfo the user info component of the new URI
         * @param host     the host component of the new URI
         * @param port     the port component of the new URI
         * @param path     the path component of the new URI
         * @param query    the query component of the new URI
         * @param fragment the fragment component of the new URI
         * @return a new URI object with the specified components
         * @throws Exception if an error occurs while constructing the URI
         */
        public URI create(String scheme, String userInfo, String host, int port, String path, String query, String fragment) throws Exception {
            URI result = constructor.newInstance();
            schemeField.set(result, scheme);
            userInfoField.set(result, userInfo);
            hostField.set(result, host);
            portField.set(result, port);
            pathField.set(result, path);
            queryField.set(result, query);
            fragmentField.set(result, fragment);
            authorityField.set(result, authority(userInfo, host, port));
            return result;
        }

        /**
         * Constructs the authority component of a URI from the user info, host, and port components.
         *
         * @param userInfo the user info component
         * @param host     the host component
         * @param port     the port component
         * @return the authority component of the URI
         */
        private String authority(String userInfo, String host, int port) {
            if (userInfo == null || userInfo.isEmpty()) {
                if (port <= 0) {
                    return host == null ? "" : host;
                } else {
                    return (host == null ? "" : host) + ":" + port;
                }
            }
            StringBuilder builder = new StringBuilder(128);
            builder.append(userInfo).append("@");
            if (host != null) {
                builder.append(host);
            }
            if (port > 0) {
                builder.append(":").append(port);
            }
            return builder.toString();
        }
    }
}

