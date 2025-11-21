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

import com.jd.live.agent.core.parser.annotation.JsonAlias;

/**
 * Enumerates the scopes for HTTP data storage locations.
 * <p>
 * This enum defines three different locations where HTTP data can be stored: HEADER, QUERY, and COOKIE.
 * The {@link JsonAlias} annotation specifies the alias used during JSON serialization and deserialization
 * for each enum value.
 * </p>
 */
public enum HttpScope {

    /**
     * Represents the HTTP header storage location.
     * <p>
     * Use this scope to specify that the data should be stored in or retrieved from the HTTP request or response headers.
     * </p>
     */
    @JsonAlias("header")
    HEADER,

    /**
     * Represents the HTTP query parameter storage location.
     * <p>
     * Use this scope to specify that the data should be stored in or retrieved from the query parameters of the HTTP request URL.
     * </p>
     */
    @JsonAlias("query")
    QUERY,

    /**
     * Represents the HTTP cookie storage location.
     * <p>
     * Use this scope to specify that the data should be stored in or retrieved from the cookies of the HTTP request or response.
     * </p>
     */
    @JsonAlias("cookie")
    COOKIE
}