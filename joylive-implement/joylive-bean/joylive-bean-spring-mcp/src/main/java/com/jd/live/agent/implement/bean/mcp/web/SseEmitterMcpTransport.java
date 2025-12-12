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
package com.jd.live.agent.implement.bean.mcp.web;

import com.jd.live.agent.core.mcp.McpTransport;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Implementation of McpTransport using Spring's SseEmitter for Server-Sent Events.
 * <p>
 * This class provides a transport layer for MCP (Model Context Protocol) communication
 * over HTTP using Server-Sent Events (SSE). It manages the lifecycle of SSE connections
 * and handles session creation, message delivery, and connection termination.
 *
 * @see McpTransport The transport interface this class implements
 * @see SseEmitter Spring's Server-Sent Events emitter
 */
public class SseEmitterMcpTransport implements McpTransport {

    private final SseEmitter emitter;
    private final String id;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile long lastAccessedTime;

    /**
     * Creates a new SSE-based MCP transport.
     *
     * @param emitter The Spring SseEmitter for sending server events
     * @param id      The unique identifier for the client connection
     */
    public SseEmitterMcpTransport(SseEmitter emitter, String id) {
        this.emitter = emitter;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public <T> T getConnection() {
        return (T) emitter;
    }

    @Override
    public CompletionStage<Void> send(String id, EventType type, Object data) {
        lastAccessedTime = System.currentTimeMillis();
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            if (closed.get()) {
                result.completeExceptionally(new IllegalStateException("SSE connection is already closed for client: " + this.id));
            } else {
                switch (type) {
                    case HEARTBEAT:
                        emitter.send(SseEmitter.event().comment(type.getValue()));
                        break;
                    case ENDPOINT:
                    case MESSAGE:
                    default:
                        emitter.send(SseEmitter.event().id(id).name(type.getValue()).data(data));
                }
                result.complete(null);
            }
        } catch (IOException e) {
            close(() -> emitter.completeWithError(e));
            result.completeExceptionally(e);
        }
        return result;
    }

    @Override
    public CompletionStage<Void> close() {
        return close(() -> emitter.complete());
    }

    @Override
    public CompletionStage<Void> close(Throwable cause) {
        return close(() -> emitter.completeWithError(cause));
    }

    @Override
    public void onCompletion(Runnable runnable) {
        emitter.onCompletion(runnable);
    }

    @Override
    public void onError(Consumer<Throwable> consumer) {
        emitter.onError(consumer);
    }

    @Override
    public void onTimeout(Runnable runnable) {
        emitter.onTimeout(runnable);
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    /**
     * Internal method to close the transport with a follow-up action.
     *
     * @param thenRun Action to execute after closing the transport
     * @return A CompletionStage that completes when the close operation is done
     */
    private CompletionStage<Void> close(Runnable thenRun) {
        if (closed.compareAndSet(false, true)) {
            thenRun.run();
        }
        return CompletableFuture.completedFuture(null);
    }
}
