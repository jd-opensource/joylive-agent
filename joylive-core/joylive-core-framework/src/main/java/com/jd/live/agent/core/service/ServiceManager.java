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

import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.AgentEvent.EventType;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The ServiceManager class is responsible for managing a collection of services that implement the
 * AgentService interface. It provides facilities to start and stop all managed services in an
 * orderly fashion and publishes events to indicate the service lifecycle status.
 *
 * @since 1.0.0
 */
public class ServiceManager implements AgentService {

    /**
     * The publisher used to distribute AgentEvent instances when services start or stop.
     */
    private final Publisher<AgentEvent> publisher;

    /**
     * The list of services that this manager is responsible for.
     */
    private final List<AgentService> services;

    /**
     * Constructs a new ServiceManager with the given services and event publisher.
     *
     * @param services the list of services to manage.
     * @param publisher the publisher used to send out AgentEvent instances.
     */
    public ServiceManager(List<AgentService> services, Publisher<AgentEvent> publisher) {
        this.services = services;
        this.publisher = publisher;
        addShutdownHook();
    }

    protected void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop().join()));
    }

    @Override
    public CompletableFuture<Void> start() {
        return execute(AgentService::start, s -> new AgentEvent(EventType.AGENT_SERVICE_START,
                "service " + s.getClass().getSimpleName() + " is started."));
    }

    @Override
    public CompletableFuture<Void> stop() {
        return execute(AgentService::stop, s -> new AgentEvent(EventType.AGENT_SERVICE_STOP,
                "service " + s.getClass().getSimpleName() + " is stopped."));
    }

    /**
     * Executes an action (start or stop) on all managed services and publishes success events.
     *
     * @param actionFunc the action to apply to each service.
     * @param successFunc the function to create a success AgentEvent after the action is completed.
     * @return a CompletableFuture that completes when all services have executed the action.
     */
    private CompletableFuture<Void> execute(Function<AgentService, CompletableFuture<Void>> actionFunc,
                                            Function<AgentService, AgentEvent> successFunc) {
        CompletableFuture<?>[] futures = new CompletableFuture[services.size()];
        int index = 0;
        for (AgentService service : services) {
            futures[index++] = actionFunc.apply(service).whenComplete((r, t) -> {
                if (t == null) {
                    publisher.offer(new Event<>(successFunc.apply(service)));
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
