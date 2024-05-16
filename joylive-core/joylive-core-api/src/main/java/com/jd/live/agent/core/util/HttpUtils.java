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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Utility class for handling HTTP requests. Provides convenience methods to send HTTP GET requests
 * and process the responses.
 * <p>
 * This class simplifies the connection setup and response handling by encapsulating the necessary
 * steps to configure {@link HttpURLConnection}, execute the request, and process the incoming data.
 * It deals with common setup configurations and response content encodings such as gzip and deflate.
 * <p>
 * Example usage:
 * <pre>
 *     String result = HttpUtils.get("http://example.com/api/data", connection -> {
 *         // Custom connection configuration
 *         connection.setConnectTimeout(10000);
 *         connection.setReadTimeout(10000);
 *     }, reader -> {
 *         // Process the response with BufferedReader
 *         return reader.lines().collect(Collectors.joining("\n"));
 *     });
 * </pre>
 * <p>
 * The class is designed to be flexible and allows for custom configuration and processing steps to be
 * provided by the caller.
 */
public class HttpUtils {

    /**
     * Executes an HTTP GET request to the specified URI, allowing for custom configuration of the
     * {@link HttpURLConnection} and processing of the response using a provided function.
     *
     * @param <T>       the type of the object returned by the processing function
     * @param uri       the URI to send the GET request to
     * @param configure a {@link Consumer} that accepts an {@link HttpURLConnection} to apply additional configuration
     * @param function  a {@link Function} that takes a {@link Reader} and returns an object of type {@code T} after processing the response
     * @return an object of type {@code T} as returned by the provided {@code function}
     * @throws IOException if an I/O error occurs during the connection or if the HTTP response code is not {@link HttpURLConnection#HTTP_OK}
     */
    public static <T> T get(String uri, Consumer<HttpURLConnection> configure, Function<Reader, T> function) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("contentType", "UTF-8");
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(true);
            configure.accept(connection);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                String encoding = connection.getContentEncoding();
                if (encoding != null && encoding.contains("gzip")) {
                    is = new GZIPInputStream(is);
                } else if (encoding != null && encoding.contains("deflate")) {
                    is = new InflaterInputStream(is, new Inflater(true));
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return function.apply(reader);
                }
            }
            throw new IOException("Failed to get " + uri + ". http response code:" + responseCode);
        } finally {
            connection.disconnect();
        }
    }
}
