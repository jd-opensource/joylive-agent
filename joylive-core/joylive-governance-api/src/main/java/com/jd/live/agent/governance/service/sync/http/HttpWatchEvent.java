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
package com.jd.live.agent.governance.service.sync.http;

import lombok.Getter;

import java.io.IOException;

/**
 * A class representing an HTTP event.
 */
@Getter
public class HttpWatchEvent {
    /**
     * The type of the HTTP event.
     */
    private final EventType type;

    /**
     * The URL associated with the HTTP event.
     */
    private final String id;

    /**
     * The content of the HTTP response, if applicable.
     */
    private final String data;

    /**
     * The exception that occurred during the HTTP request, if applicable.
     */
    private final IOException throwable;


    public HttpWatchEvent(EventType type, String id, String data) {
        this(type, id, data, null);
    }

    public HttpWatchEvent(String id, IOException throwable) {
        this(EventType.ERROR, id, null, throwable);
    }

    public HttpWatchEvent(EventType type, String id, String data, IOException throwable) {
        this.type = type;
        this.id = id;
        this.data = data;
        this.throwable = throwable;
    }

    /**
     * An enumeration representing the possible types of HTTP events.
     */
    public enum EventType {
        /**
         * Indicates that the HTTP resource has been updated.
         */
        UPDATE,

        /**
         * Indicates that the HTTP resource has been deleted.
         */
        DELETE,

        /**
         * Indicates that an error occurred during the HTTP request.
         */
        ERROR,
    }
}
