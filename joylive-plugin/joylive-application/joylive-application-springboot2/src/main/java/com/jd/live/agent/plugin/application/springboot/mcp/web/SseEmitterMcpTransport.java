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
package com.jd.live.agent.plugin.application.springboot.mcp.web;

import com.jd.live.agent.core.mcp.McpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterMcpTransport.class);

    private final SseEmitter emitter;
    private final String sessionId;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Function<String, CompletionStage<Void>> closer;

    /**
     * Creates a new SSE-based MCP transport.
     *
     * @param emitter        The Spring SseEmitter for sending server events
     * @param sessionId      The unique identifier for the client connection
     * @param closer         The closer
     */
    public SseEmitterMcpTransport(SseEmitter emitter, String sessionId, Function<String, CompletionStage<Void>> closer) {
        this.emitter = emitter;
        this.sessionId = sessionId;
        this.closer = closer;
        emitter.onCompletion(this::onCompletion);
        emitter.onTimeout(this::onTimeout);
    }

    @Override
    public <T> T getConnection() {
        return (T) emitter;
    }

    @Override
    public CompletionStage<Void> send(String id, EventType type, Object data) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            if (closed.get()) {
                result.completeExceptionally(new IllegalStateException("SSE connection is already closed for client: " + sessionId));
            } else {
                MediaType mediaType = type == EventType.ENDPOINT ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_JSON;
                emitter.send(SseEmitter.event().id(id).name(type.getName()).data(data, mediaType));
                result.complete(null);
            }
        } catch (IOException e) {
            close();
            result.completeExceptionally(e);
        }
        return result;
    }

    @Override
    public CompletionStage<Void> close() {
        return close(() -> emitter.complete());
    }

    /**
     * Handles the SSE connection completion event.
     * Closes the transport and logs the completion.
     */
    private void onCompletion() {
        close(() -> logger.info("SSE connection is completed for session: {}", sessionId));
    }

    /**
     * Handles the SSE connection timeout event.
     * Closes the transport and logs the timeout.
     */
    private void onTimeout() {
        close(() -> {
            emitter.complete();
            logger.warn("SSE connection is timed out for session: {}", sessionId);
        });
    }

    /**
     * Internal method to close the transport with a follow-up action.
     *
     * @param thenRun Action to execute after closing the transport
     * @return A CompletionStage that completes when the close operation is done
     */
    private CompletionStage<Void> close(Runnable thenRun) {
        if (closed.compareAndSet(false, true)) {
            if (closer != null) {
                return closer.apply(sessionId).thenRun(thenRun);
            }
            thenRun.run();
        }
        return CompletableFuture.completedFuture(null);
    }
}
