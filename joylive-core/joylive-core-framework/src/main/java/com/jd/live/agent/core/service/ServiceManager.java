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
package com.jd.live.agent.core.service;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.AgentEvent.EventType;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.Executors;
import com.jd.live.agent.core.util.shutdown.GracefullyShutdown;
import com.jd.live.agent.core.util.shutdown.ShutdownHook;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.jd.live.agent.core.util.shutdown.GracefullyShutdown.getMaxWaitTime;

/**
 * The ServiceManager class is responsible for managing a collection of services that implement the
 * AgentService interface. It provides facilities to start and stop all managed services in an
 * orderly fashion and publishes events to indicate the service lifecycle status.
 *
 * @since 1.0.0
 */
@Injectable
public class ServiceManager implements ServiceSupervisor, ShutdownHook, GracefullyShutdown, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    /**
     * The publisher used to distribute AgentEvent instances when services start or stop.
     */
    @Inject(Publisher.SYSTEM)
    private Publisher<AgentEvent> publisher;

    /**
     * The list of services that this manager is responsible for.
     */
    @Inject
    private List<AgentService> services;

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public List<AgentService> getServices() {
        return services;
    }

    /**
     * Starts all services asynchronously. Returns immediately if already started.
     * Upon successful start, publishes AGENT_SERVICE_READY event.
     *
     * @return Future that completes when services are started,
     * or completed future if already running
     */
    public CompletableFuture<Void> start() {
        if (started.compareAndSet(false, true)) {
            return execute(this::startService, s -> "Service " + s.getName() + " is started.").whenComplete((v, t) -> {
                if (t == null) {
                    publisher.offer(new AgentEvent(EventType.AGENT_SERVICE_READY, "All services are started."));
                }
            });
        } else {
            return CompletableFuture.completedFuture(null);
        }

    }

    /**
     * Stops all services asynchronously. Returns immediately if not running.
     *
     * @return Future that completes when services are stopped,
     * or completed future if already stopped
     */
    public CompletableFuture<Void> stop() {
        if (started.compareAndSet(true, false)) {
            return execute(AgentService::stop, s -> "Service " + s.getName() + " is stopped.");
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Gracefully shuts down the service.
     */
    @Override
    public void close() {
        try {
            stop().join();
        } catch (Throwable e) {
            logger.warn("Failed to shutdown service, caused by " + e.getMessage());
        }
    }

    @Override
    public int getWaitTime() {
        return getMaxWaitTime(services);
    }

    /**
     * Starts the given AgentService asynchronously.
     *
     * @param service The AgentService to start.
     * @return A CompletableFuture that represents the asynchronous start operation of the AgentService.
     */
    private CompletableFuture<Void> startService(AgentService service) {
        return Executors.get(service.getClass().getClassLoader(), () -> service.start());
    }

    /**
     * Executes an action (start or stop) on all managed services and publishes success events.
     *
     * @param actionFunc the action to apply to each service.
     * @param successFunc the function to create a success AgentEvent after the action is completed.
     * @return a CompletableFuture that completes when all services have executed the action.
     */
    private CompletableFuture<Void> execute(Function<AgentService, CompletableFuture<Void>> actionFunc,
                                            Function<AgentService, String> successFunc) {
        CompletableFuture<?>[] futures = new CompletableFuture[services.size()];
        int index = 0;
        for (AgentService service : services) {
            futures[index++] = actionFunc.apply(service).whenComplete((r, t) -> {
                if (t == null) {
                    logger.info(successFunc.apply(service));
                }
            });
        }
        if (futures.length == 0)
            return CompletableFuture.completedFuture(null);
        else {
            return CompletableFuture.allOf(futures);
        }

    }
}
