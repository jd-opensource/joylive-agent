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
package com.jd.live.agent.implement.bean.mcp.web.reactive;

import com.jd.live.agent.core.mcp.McpTransport;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ReactiveMcpTransport implements McpTransport {

    private final Sinks.Many<ServerSentEvent<Object>> sink;
    private final String id;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile long lastAccessedTime;

    public ReactiveMcpTransport(Sinks.Many<ServerSentEvent<Object>> sink, String id) {
        this.sink = sink;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public <T> T getConnection() {
        return (T) sink;
    }

    @Override
    public CompletionStage<Void> send(String id, EventType type, Object data) {
        lastAccessedTime = System.currentTimeMillis();
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (closed.get()) {
            result.completeExceptionally(new IllegalStateException("SSE connection is already closed for client: " + this.id));
        } else {
            ServerSentEvent<Object> event;
            switch (type) {
                case HEARTBEAT:
                    event = ServerSentEvent.builder().comment(type.getValue()).build();
                    break;
                case ENDPOINT:
                case MESSAGE:
                default:
                    event = ServerSentEvent.builder().id(id).event(type.getValue()).data(data).build();
            }
            Sinks.EmitResult er = sink.tryEmitNext(event);
            if (er.isSuccess()) {
                result.complete(null);
            } else {
                sink.tryEmitError(new IOException("Failed to send message for session: " + this.id));
                result.completeExceptionally(new IOException("Failed to send message for session: " + this.id));
            }
        }
        return result;
    }

    @Override
    public CompletionStage<Void> close() {
        return close(() -> sink.tryEmitComplete());
    }

    @Override
    public void onCompletion(Runnable runnable) {
        sink.asFlux().doOnComplete(runnable).subscribe();
    }

    @Override
    public void onError(Consumer<Throwable> consumer) {
        sink.asFlux().doOnError(consumer).subscribe();
    }

    @Override
    public void onTimeout(Runnable runnable) {

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
