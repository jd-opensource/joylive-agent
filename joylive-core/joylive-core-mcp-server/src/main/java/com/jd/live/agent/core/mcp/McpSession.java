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

import com.jd.live.agent.core.mcp.spec.v1.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * Interface for managing MCP (Model Context Protocol) sessions.
 */
public interface McpSession {

    String HEADER_SESSION_ID = "Mcp-Session-Id";

    String HEADER_PROTOCOL_VERSION = "MCP-Protocol-Version";

    String QUERY_SESSION_ID = "sessionId";

    /**
     * Returns the unique identifier for this session.
     *
     * @return The session ID
     */
    String getId();

    /**
     * Returns the MCP protocol version used by this session.
     *
     * @return The version identifier string of the MCP protocol
     */
    String getVersion();

    /**
     * Handles initialization requests from clients.
     *
     * @param request The initialization request to process
     */
    InitializeResult initialize(InitializeRequest request);

    /**
     * Returns the current logging level for this session.
     *
     * @return The current LoggingLevel setting
     */
    LoggingLevel getLoggingLevel();

    /**
     * Updates the session's logging level.
     *
     * @param level The new LoggingLevel to apply
     */
    void setLoggingLevel(LoggingLevel level);

    /**
     * Returns the timestamp of the most recent session activity.
     *
     * @return The last accessed time in milliseconds since epoch
     */
    long getLastAccessedTime();

    /**
     * Updates the timestamp of the most recent session activity.
     *
     * @param lastAccessedTime The timestamp in milliseconds since epoch
     */
    void setLastAccessedTime(long lastAccessedTime);

    /**
     * Processes initialization completion notifications.
     *
     * @param notification The initialization notification to handle
     */
    void inform(InitializedNotification notification);

    /**
     * Checks if this session has been initialized.
     *
     * @return true if the session is initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * Closes the transport connection asynchronously.
     *
     * @return A CompleteFuture that completes when the connection is closed
     */
    CompletionStage<Void> close();

    /**
     * Default implementation of the McpSession interface.
     * Handles MCP session management, request processing, and notification handling.
     */
    class DefaultMcpSession implements McpSession {
        private final String id;
        private final ServerCapabilities serverCapabilities;
        private final Implementation serverInfo;
        private final Predicate<String> versionPredicate;
        private ClientCapabilities clientCapabilities;
        private Implementation clientInfo;
        private LoggingLevel loggingLevel;

        private String version;
        private long lastAccessedTime;
        private volatile boolean initialized;

        public DefaultMcpSession(String id,
                                 String version,
                                 ServerCapabilities serverCapabilities,
                                 Implementation serverInfo,
                                 Predicate<String> idPredicate) {
            this.id = id;
            this.version = version;
            this.serverCapabilities = serverCapabilities;
            this.serverInfo = serverInfo;
            this.versionPredicate = idPredicate;
            this.lastAccessedTime = System.currentTimeMillis();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getVersion() {
            return version != null ? version : serverInfo.getVersion();
        }

        @Override
        public InitializeResult initialize(InitializeRequest request) {
            this.clientCapabilities = request.getCapabilities();
            this.clientInfo = request.getClientInfo();
            String protocolVersion = request.getProtocolVersion();
            this.version = versionPredicate.test(protocolVersion) ? protocolVersion : version;
            return InitializeResult.builder()
                    .protocolVersion(version)
                    .capabilities(serverCapabilities)
                    .serverInfo(serverInfo)
                    .instructions(null)
                    .meta(null)
                    .build();
        }

        @Override
        public LoggingLevel getLoggingLevel() {
            return loggingLevel;
        }

        @Override
        public void setLoggingLevel(LoggingLevel loggingLevel) {
            this.loggingLevel = loggingLevel;
        }

        @Override
        public long getLastAccessedTime() {
            return lastAccessedTime;
        }

        @Override
        public void setLastAccessedTime(long lastAccessedTime) {
            this.lastAccessedTime = lastAccessedTime;
        }

        @Override
        public void inform(InitializedNotification notification) {
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public CompletionStage<Void> close() {
            return CompletableFuture.completedFuture(null);
        }
    }

}
