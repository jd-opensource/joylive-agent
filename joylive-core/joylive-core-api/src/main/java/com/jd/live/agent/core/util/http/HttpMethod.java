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
 * Enumerates the HTTP methods, providing a method to identify if the HTTP method implies writing data.
 * <p>
 * This enumeration defines all standard HTTP methods and extends some of them with the ability to distinguish between
 * methods that are generally used for writing data (such as POST, PUT, PATCH, and DELETE) and those that are not.
 * </p>
 */
public enum HttpMethod {

    /**
     * The HTTP GET method requests a representation of the specified resource.
     * Requests using GET should only retrieve data.
     */
    GET,

    /**
     * The HTTP HEAD method asks for a response identical to a GET request, but without the response body.
     */
    HEAD,

    /**
     * The HTTP POST method is used to submit an entity to the specified resource,
     * often causing a change in state or side effects on the server.
     */
    POST {
        @Override
        public boolean isWrite() {
            return true;
        }
    },

    /**
     * The HTTP PUT method replaces all current representations of the target resource with the request payload.
     */
    PUT {
        @Override
        public boolean isWrite() {
            return true;
        }
    },

    /**
     * The HTTP PATCH method is used to apply partial modifications to a resource.
     */
    PATCH {
        @Override
        public boolean isWrite() {
            return true;
        }
    },

    /**
     * The HTTP DELETE method deletes the specified resource.
     */
    DELETE {
        @Override
        public boolean isWrite() {
            return true;
        }
    },

    /**
     * The HTTP OPTIONS method describes the communication options for the target resource.
     */
    OPTIONS,

    /**
     * The HTTP TRACE method performs a message loop-back test along the path to the target resource.
     */
    TRACE;

    /**
     * Determines if the HTTP method implies writing data.
     * <p>
     * By default, this method returns {@code false}, indicating that the method does not imply writing data.
     * This method is overridden by HTTP methods that do imply writing data.
     * </p>
     *
     * @return {@code true} if the HTTP method implies writing data; {@code false} otherwise.
     */
    public boolean isWrite() {
        return false;
    }
}

