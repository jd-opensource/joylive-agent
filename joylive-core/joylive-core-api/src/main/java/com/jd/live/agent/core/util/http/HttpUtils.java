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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
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

}
