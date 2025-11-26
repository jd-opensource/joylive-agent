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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages MCP session lifecycle and storage.
 * Provides thread-safe operations for session tracking.
 */
public interface McpSessionManager {

    /**
     * Retrieves a session by its ID.
     *
     * @param sessionId The unique session identifier
     * @return The session if found, null otherwise
     */
    McpSession get(String sessionId);

    /**
     * Stores a session with the specified ID.
     *
     * @param sessionId The unique session identifier
     * @param session   The session to store
     */
    void put(String sessionId, McpSession session);

    /**
     * Removes a session by its ID.
     *
     * @param sessionId The unique session identifier to remove
     */
    McpSession remove(String sessionId);

    /**
     * Gracefully shuts down the session manager.
     *
     * @return A CompletionStage that completes when shutdown is finished
     */
    CompletionStage<Void> close();

    /**
     * Removes expired sessions based on time-to-live.
     *
     * @param ttl Maximum idle time in milliseconds before a session is evicted
     */
    int evict(long ttl);

    class DefaultMcpSessionManager implements McpSessionManager {

        private Map<String, McpSession> sessions = new ConcurrentHashMap<>();

        @Override
        public McpSession get(String sessionId) {
            return sessionId == null ? null : sessions.get(sessionId);
        }

        @Override
        public void put(String sessionId, McpSession session) {
            sessions.put(sessionId, session);
        }

        @Override
        public McpSession remove(String sessionId) {
            return sessionId == null ? null : sessions.remove(sessionId);
        }

        @Override
        public int evict(long ttl) {
            if (sessions.isEmpty()) {
                return 0;
            }
            int count = 0;
            long current = System.currentTimeMillis();
            long expireTime = current - ttl;
            for (Map.Entry<String, McpSession> entry : sessions.entrySet()) {
                McpSession session = entry.getValue();
                if (session.getLastAccessedTime() > expireTime) {
                    count++;
                    session.close();
                }
            }
            return count;
        }

        @Override
        public CompletionStage<Void> close() {
            CompletableFuture result = new CompletableFuture();
            AtomicInteger counter = new AtomicInteger(sessions.size());
            for (Map.Entry<String, McpSession> entry : sessions.entrySet()) {
                entry.getValue().close().whenComplete((r, e) -> {
                    if (counter.decrementAndGet() == 0) {
                        result.complete(null);
                    }
                });
            }
            if (counter.get() == 0) {
                result.complete(null);
            }
            return result;
        }
    }
}
