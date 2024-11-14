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
package com.jd.live.agent.governance.registry;

import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.AgentEvent.EventType;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.EventHandler.EventProcessor;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.service.sync.AbstractService;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code LiveRegistry} is an implementation of {@link Registry} that manages the registration and unregistration
 * of service instances. It also handles agent events to determine the readiness of the registry and manages
 * heartbeat signals to ensure service instances are alive.
 *
 * @see AbstractService
 * @see Registry
 * @see InjectSourceSupplier
 */
@Extension("LiveRegistry")
@Injectable
public class LiveRegistry extends AbstractService implements Registry, InjectSourceSupplier {

    @Inject(Publisher.REGISTRY)
    private Publisher<RegistryEvent> registryPublisher;

    @Inject(Publisher.SYSTEM)
    private Publisher<AgentEvent> systemPublisher;

    @Inject(RegistryConfig.COMPONENT_REGISTRY_CONFIG)
    private RegistryConfig registryConfig;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    private final EventHandler<AgentEvent> readyHandler = (EventProcessor<AgentEvent>) this::onAgentEvent;

    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @Override
    protected CompletableFuture<Void> doStart() {
        systemPublisher.addHandler(readyHandler);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        ready.set(false);
        systemPublisher.removeHandler(readyHandler);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void register(ServiceInstance instance) {
        if (instance != null) {
            Registration registration = new Registration(instance);
            Registration old = registrations.putIfAbsent(instance.getService(), registration);
            if (old == null) {
                if (ready.get()) {
                    doRegister(registration);
                }
            }
        }
    }

    @Override
    public void unregister(ServiceInstance instance) {
        if (instance != null) {
            Registration registration = registrations.remove(instance.getService());
            doUnregister(registration);
        }
    }

    /**
     * Handles {@link AgentEvent}s to update the readiness state of the registry.
     *
     * @param event the agent event to handle.
     */
    private void onAgentEvent(AgentEvent event) {
        if (event.getType() == EventType.AGENT_READY) {
            ready.set(true);
            onReady();
        } else {
            ready.set(false);
        }
    }

    /**
     * Called when the registry becomes ready to register all pending registrations.
     */
    private void onReady() {
        for (Registration registration : registrations.values()) {
            doRegister(registration);
        }
    }

    /**
     * Performs the actual registration of a {@link Registration}.
     *
     * @param registration the registration to perform.
     */
    private void doRegister(Registration registration) {
        if (registration != null) {
            synchronized (registration) {
                if (ready.get() && !registration.registered) {
                    registryPublisher.offer(RegistryEvent.register(registration.instance));
                    registration.registered = true;
                    addHeartbeat(registration);
                }
            }
        }
    }

    private void doUnregister(Registration registration) {
        if (registration != null) {
            synchronized (registration) {
                if (registration.registered) {
                    registryPublisher.offer(RegistryEvent.unregister(registration.instance));
                }
            }
        }
    }

    /**
     * Performs the actual unregistration of a {@link Registration}.
     *
     * @param registration the registration to perform.
     */
    private void addHeartbeat(Registration registration) {
        if (registration != null) {
            long delay = registryConfig.getHeartbeatInterval() + (long) (Math.random() * 2000.0);
            timer.delay("registration-" + registration.getService(), delay, () -> doHeartbeat(registration));
        }
    }

    /**
     * Adds a heartbeat task for the given {@link Registration}.
     *
     * @param registration the registration to add a heartbeat for.
     */
    private void doHeartbeat(Registration registration) {
        if (registration != null) {
            synchronized (registration) {
                if (ready.get() && registrations.containsKey(registration.getService())) {
                    registryPublisher.offer(RegistryEvent.heartbeat(registration.instance));
                    addHeartbeat(registration);
                }
            }
        }
    }

    @Override
    public void apply(InjectSource source) {
        source.add(Registry.COMPONENT_REGISTRY, this);
    }

    /**
     * Represents a registration of a {@link ServiceInstance}.
     */
    private static class Registration {

        protected boolean registered;

        protected ServiceInstance instance;

        Registration(ServiceInstance instance) {
            this.instance = instance;
        }

        public String getService() {
            return instance.getService();
        }
    }
}

