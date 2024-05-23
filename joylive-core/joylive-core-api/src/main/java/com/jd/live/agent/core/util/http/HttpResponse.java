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

import lombok.Getter;

/**
 * Represents an HTTP response with a status code, an optional message, and optional data of a generic type.
 *
 * @param <T> The type of the data contained in the response.
 */
@Getter
public class HttpResponse<T> implements HttpState {

    /**
     * The HTTP status code of the response.
     */
    private final HttpStatus status;

    /**
     * The message associated with the response. This might be an error message or status information.
     */
    private final String message;

    /**
     * The data of the response. This is generic and can be any type of object that represents the body of the response.
     */
    private final T data;

    /**
     * Constructs a new HttpResponse with the specified status code and data, but without a message.
     *
     * @param status The HTTP status code.
     * @param data   The data of the response.
     */
    public HttpResponse(HttpStatus status, T data) {
        this(status, null, data);
    }

    /**
     * Constructs a new HttpResponse with the specified status code and message, but without data.
     *
     * @param status  The HTTP status code.
     * @param message The message associated with the response.
     */
    public HttpResponse(HttpStatus status, String message) {
        this(status, message, null);
    }

    /**
     * Constructs a new HttpResponse with the specified status code, message, and data.
     *
     * @param status  The HTTP status code.
     * @param message The message associated with the response.
     * @param data    The data of the response.
     */
    public HttpResponse(HttpStatus status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    @Override
    public int getCode() {
        return status.value();
    }
}
