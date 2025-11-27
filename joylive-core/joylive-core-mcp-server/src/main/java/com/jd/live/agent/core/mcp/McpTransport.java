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
package com.jd.live.agent.core.mcp;

import java.util.concurrent.CompletionStage;

/**
 * Transport interface for sending JSON-RPC messages in MCP (Model Context Protocol).
 * <p>
 * Provides asynchronous methods for message transmission and connection management.
 */
public interface McpTransport {

    /**
     * Retrieves the underlying connection object.
     *
     * @param <T> The connection type
     * @return The connection instance
     */
    <T> T getConnection();

    /**
     * Sends a server-sent event with specified ID, type, and data payload.
     *
     * @param id   The unique identifier for the event
     * @param type The type of event being sent
     * @param data The payload data to send with the event
     * @return A CompletionStage that completes when the event is sent
     */
    CompletionStage<Void> send(String id, EventType type, Object data);

    /**
     * Closes the transport connection asynchronously.
     *
     * @return A CompleteFuture that completes when the connection is closed
     */
    CompletionStage<Void> close();

    enum EventType {
        /**
         * Event type for sending the message endpoint URI to clients.
         */
        ENDPOINT("endpoint"),

        /**
         * Event type for regular messages
         */
        MESSAGE("message");

        private String name;

        EventType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
