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


/**
 * Represents the state of an HTTP response.
 * This interface defines methods to access the status code and message
 * associated with an HTTP response.
 */
public interface HttpState {

    /**
     * Retrieves the HTTP status code.
     * The status code is intended to provide a numeric representation of
     * the HTTP response status, indicating the outcome of the HTTP request.
     *
     * @return An {@code int} representing the HTTP status code.
     */
    int getCode();

    /**
     * Retrieves the HTTP status message.
     * The status message provides a textual description associated with
     * the HTTP status code, offering additional context about the response.
     *
     * @return A {@code String} representing the HTTP status message.
     */
    String getMessage();

}
