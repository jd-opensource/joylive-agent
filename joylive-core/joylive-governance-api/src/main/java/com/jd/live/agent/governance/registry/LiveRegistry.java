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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bootstrap.AppListenerSupplier;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryConfig;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.policy.PolicySupplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.jd.live.agent.governance.registry.RegistryEvent.*;

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
public class LiveRegistry extends AbstractService implements RegistrySupervisor, InjectSourceSupplier, AppListenerSupplier {

    private static final Logger logger = LoggerFactory.getLogger(LiveRegistry.class);

    @Inject(Publisher.REGISTRY)
    private Publisher<RegistryEvent> publisher;

    @Inject(RegistryConfig.COMPONENT_REGISTRY_CONFIG)
    private RegistryConfig registryConfig;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @Override
    protected CompletableFuture<Void> doStart() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        ready.set(false);
        onApplicationStop();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public AppListener getAppListener() {
        return new AppListener.AppListenerAdapter() {

            @Override
            public void onReady(AppContext context) {
                onApplicationReady();
            }

            public void onStop(AppContext context) {
                onApplicationStop();
            }
        };
    }

    @Override
    public void register(ServiceInstance instance, Callable<Void> callback) {
        if (instance != null) {
            String service = instance.getService();
            Registration registration = registrations.computeIfAbsent(service,
                    name -> new Registration(instance, callback, publisher, registryConfig, timer));
            if (ready.get()) {
                // delay register
                registration.register();
            } else {
                logger.info("Delay registration until application is ready, service=" + service);
            }
        }
    }

    @Override
    public void unregister(ServiceInstance instance) {
        if (instance != null) {
            Registration registration = registrations.remove(instance.getService());
            if (registration != null) {
                registration.unregister();
            }
        }
    }

    @Override
    public CompletableFuture<Void> subscribe(String service) {
        return policySupplier.subscribe(service);
    }

    @Override
    public void subscribe(String service, Consumer<EndpointEvent> consumer) {
        if (service != null && !service.isEmpty() && consumer != null) {
            Subscription subscription = subscriptions.computeIfAbsent(service, Subscription::new);
            subscription.addConsumer(consumer);
        }
    }

    @Override
    public void update(String service, List<? extends Endpoint> endpoints) {
        if (service != null && !service.isEmpty()) {
            Subscription subscription = subscriptions.get(service);
            if (subscription != null) {
                subscription.update(endpoints);
            }
        }
    }

    @Override
    public boolean isSubscribed(String service) {
        return service != null && !service.isEmpty() && subscriptions.containsKey(service);
    }

    /**
     * Called when the application is ready to start. This method iterates through all registered services and calls their register method.
     */
    private void onApplicationReady() {
        ready.set(true);
        for (Registration registration : registrations.values()) {
            registration.register();
        }
    }

    /**
     * Called when the application is stopping. This method iterates through all registered services and calls their stop method.
     */
    private void onApplicationStop() {
        ready.set(false);
        for (Registration registration : registrations.values()) {
            registration.stop();
        }
    }

    @Override
    public void apply(InjectSource source) {
        source.add(Registry.COMPONENT_REGISTRY, this);
    }

    /**
     * A private static class that represents a registration of a service instance with the registry.
     */
    private static class Registration {

        /**
         * The service to which the instance belongs.
         */
        private final String service;

        /**
         * The service instance being registered.
         */
        private final ServiceInstance instance;

        /**
         * A callback function that will be called when the registration is successful.
         */
        private final Callable<Void> callback;

        /**
         * The publisher to which registry events will be sent.
         */
        private final Publisher<RegistryEvent> publisher;

        /**
         * The configuration for the registry.
         */
        private final RegistryConfig registryConfig;

        /**
         * A timer used to schedule heartbeat and registration delays.
         */
        private final Timer timer;

        /**
         * An atomic boolean indicating whether the registration has been started.
         */
        private final AtomicBoolean started = new AtomicBoolean(true);

        /**
         * An atomic boolean indicating whether the registration has been completed.
         */
        private final AtomicBoolean registered = new AtomicBoolean(false);

        /**
         * Creates a new registration object.
         *
         * @param instance       the service instance being registered
         * @param callback       a callback function that will be called when the registration is successful
         * @param publisher      the publisher to which registry events will be sent
         * @param registryConfig the configuration for the registry
         * @param timer          a timer used to schedule heartbeat and registration delays
         */
        Registration(ServiceInstance instance,
                     Callable<Void> callback,
                     Publisher<RegistryEvent> publisher,
                     RegistryConfig registryConfig,
                     Timer timer) {
            this.service = instance.getService();
            this.instance = instance;
            this.callback = callback;
            this.publisher = publisher;
            this.registryConfig = registryConfig;
            this.timer = timer;
        }

        /**
         * Registers the service instance with the registry.
         */
        public void register() {
            if (registered.compareAndSet(false, true)) {
                if (callback != null) {
                    doRegister();
                }
            }
        }

        /**
         * Unregisters the service instance from the registry.
         */
        public void unregister() {
            if (registered.compareAndSet(true, false)) {
                doUnregister();
            }
        }

        /**
         * Stops the registration process.
         */
        public void stop() {
            started.set(false);
            unregister();
        }

        /**
         * Delays the next heartbeat by a random amount of time.
         */
        private void delayHeartbeat() {
            long delay = registryConfig.getHeartbeatInterval() + (long) (Math.random() * 2000.0);
            timer.delay("heartbeat-" + service, delay, this::doHeartbeat);
        }

        /**
         * Delays the register process by a random amount of time.
         */
        private void delayRegister() {
            long delay = 1000 + (long) (Math.random() * 2000.0);
            timer.delay("register-" + service, delay, this::doRegister);
        }

        /**
         * Performs the actual register of the service instance.
         */
        private void doRegister() {
            if (started.get()) {
                logger.info("Register when application is ready, service=" + service);
                try {
                    callback.call();
                    publisher.offer(ofRegister(instance));
                    delayHeartbeat();
                } catch (Exception e) {
                    logger.error("Register error, service=" + service + ", caused by " + e.getMessage(), e);
                    delayRegister();
                }
            }
        }

        /**
         * Performs the actual heartbeat for the service instance.
         */
        private void doHeartbeat() {
            if (started.get()) {
                publisher.offer(ofHeartbeat(instance));
                delayHeartbeat();
            }
        }

        /**
         * Performs the actual unregister of the service instance.
         */
        private void doUnregister() {
            publisher.offer(ofUnregister(instance));
        }
    }

    /**
     * A private static class that represents a subscription to endpoint events for a specific service group.
     */
    private static class Subscription {

        /**
         * The service group that this subscription is for.
         */
        private final String service;

        /**
         * The consumer that will receive endpoint events.
         */
        private final List<Consumer<EndpointEvent>> consumers = new CopyOnWriteArrayList<>();

        /**
         * A map of endpoints for the service group, keyed by their addresses.
         */
        private Map<String, Endpoint> endpoints;

        /**
         * Creates a new subscription for the specified service.
         *
         * @param service the service to subscribe to
         */
        Subscription(String service) {
            this.service = service;
        }

        /**
         * Adds a new consumer to the list of consumers that will receive endpoint events.
         *
         * @param consumer the consumer to add
         */
        public synchronized void addConsumer(Consumer<EndpointEvent> consumer) {
            if (consumer != null && !consumers.contains(consumer)) {
                consumers.add(consumer);
            }
        }

        /**
         * Updates the endpoints for the service group and notifies the consumer of any changes.
         *
         * @param endpoints the new list of endpoints for the service group
         */
        public synchronized void update(List<? extends Endpoint> endpoints) {
            Map<String, Endpoint> newEndpoints = new HashMap<>();
            Map<String, Endpoint> oldEndpoints = this.endpoints;
            List<Endpoint> adds = new ArrayList<>();
            List<Endpoint> removes = new ArrayList<>();
            if (endpoints != null) {
                for (Endpoint endpoint : endpoints) {
                    String address = endpoint.getAddress();
                    if (oldEndpoints == null || !oldEndpoints.containsKey(address)) {
                        adds.add(endpoint);
                    }
                    newEndpoints.put(address, endpoint);
                }
            }
            if (oldEndpoints != null) {
                oldEndpoints.forEach((k, v) -> {
                    if (!newEndpoints.containsKey(k)) {
                        removes.add(v);
                    }
                });
            }
            this.endpoints = newEndpoints;
            logger.info("Service instance is changed, service=" + service + ", adds=" + adds.size() + ", removes=" + removes.size());
            for (Consumer<EndpointEvent> consumer : consumers) {
                consumer.accept(new EndpointEvent(service, endpoints, adds, removes));
            }
        }
    }
}

