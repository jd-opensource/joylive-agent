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
 * Manages MCP client lifecycle and storage.
 * Provides thread-safe operations for client tracking.
 */
public interface McpTransportManager {

    /**
     * Retrieves a client by its ID.
     *
     * @param clientId The unique client identifier
     * @return The client if found, null otherwise
     */
    McpTransport get(String clientId);

    /**
     * Stores a client with the specified ID.
     *
     * @param clientId The unique client identifier
     * @param client   The client to store
     */
    void put(String clientId, McpTransport client);

    /**
     * Removes a client by its ID.
     *
     * @param clientId The unique client identifier to remove
     */
    boolean remove(String clientId);

    CompletionStage<Void> close();

    class DefaultMcpMTransportManager implements McpTransportManager {

        private Map<String, McpTransport> clients = new ConcurrentHashMap<>();

        @Override
        public McpTransport get(String clientId) {
            return clientId == null ? null : clients.get(clientId);
        }

        @Override
        public void put(String clientId, McpTransport client) {
            clients.put(clientId, client);
        }

        @Override
        public boolean remove(String clientId) {
            if (clientId != null && !clientId.isEmpty()) {
                return clients.remove(clientId) != null;
            }
            return false;
        }

        @Override
        public CompletionStage<Void> close() {
            CompletableFuture result = new CompletableFuture();
            AtomicInteger counter = new AtomicInteger(clients.size());
            for (Map.Entry<String, McpTransport> entry : clients.entrySet()) {
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
