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
import java.util.function.Consumer;

/**
 * Transport interface for sending JSON-RPC messages in MCP (Model Context Protocol).
 * <p>
 * Provides asynchronous methods for message transmission and connection management.
 */
public interface McpTransport {

    String getId();

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

    /**
     * Closes the transport connection asynchronously.
     *
     * @param cause The throwable indicating the reason for closing, or null if closing normally
     * @return A CompletionStage that completes when the connection is closed
     */
    default CompletionStage<Void> close(Throwable cause) {
        return close();
    }

    /**
     * Registers a callback to be executed when the transport completes normally.
     *
     * @param runnable The callback to execute on completion
     */
    void onCompletion(Runnable runnable);

    /**
     * Registers a callback to be executed when the transport encounters an error.
     *
     * @param consumer The callback to execute on error
     */
    void onError(Consumer<Throwable> consumer);

    /**
     * Registers a callback to be executed when the transport times out.
     *
     * @param runnable The callback to execute on timeout
     */
    void onTimeout(Runnable runnable);

    /**
     * Returns the timestamp of the most recent session activity.
     *
     * @return The last accessed time in milliseconds since epoch
     */
    long getLastAccessedTime();

    /**
     * Checks if the transport connection is idle.
     * A connection is considered idle if no activity has occurred
     * in the last 5 seconds.
     *
     * @return true if the connection is idle, false otherwise
     */
    default boolean isIdle() {
        return System.currentTimeMillis() - getLastAccessedTime() > 5000;
    }

    enum EventType {
        /**
         * Event type for sending the message endpoint URI to clients.
         */
        ENDPOINT("endpoint"),

        /**
         * Event type for regular messages
         */
        MESSAGE("message"),

        /**
         * Event type for ping
         */
        HEARTBEAT("heartbeat");

        private String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
