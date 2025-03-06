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
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.config.RegistryConfig;
import com.jd.live.agent.governance.exception.RegistryException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.registry.RegistryService.AbstractRegistryService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * {@code LiveRegistry} is an implementation of {@link Registry} that manages the registration and unregistration
 * of service instances.
 *
 * @see AbstractService
 * @see Registry
 * @see InjectSourceSupplier
 */
@Extension("LiveRegistry")
@Injectable
public class LiveRegistry extends AbstractService implements RegistrySupervisor, InjectSourceSupplier, AppListenerSupplier {

    private static final Logger logger = LoggerFactory.getLogger(LiveRegistry.class);

    @Inject(RegistryConfig.COMPONENT_REGISTRY_CONFIG)
    private RegistryConfig registryConfig;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    @Inject
    private Map<String, RegistryFactory> factories;

    private volatile List<RegistryService> registries = null;

    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @Override
    protected CompletableFuture<Void> doStart() {
        if (!registryConfig.isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        // start registries
        List<RegistryClusterConfig> clusters = registryConfig.getClusters();
        List<RegistryService> registries = new ArrayList<>();
        try {
            if (clusters != null) {
                for (RegistryClusterConfig cluster : clusters) {
                    if (cluster.validate()) {
                        RegistryFactory factory = factories.get(cluster.getType());
                        if (factory == null) {
                            throw new RegistryException("registry type " + cluster.getType() + " is not supported");
                        }
                        registries.add(factory.create(cluster));
                    }
                }
            }
            if (registries.isEmpty()) {
                throw new RegistryException("No registry config found");
            } else {
                for (RegistryService registry : registries) {
                    startCluster(registry);
                }
            }
        } catch (Throwable e) {
            return Futures.future(e);
        }
        this.registries = registries;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        ready.set(false);
        onApplicationStop();
        // stop registries
        Close.instance().close(registries);
        registries = null;
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
    public void register(ServiceInstance instance, Callable<Void> doRegister) {
        if (instance != null) {
            String service = instance.getService();
            Registration registration = registrations.computeIfAbsent(service, name -> createRegistration(instance, doRegister));
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

    @Override
    public void apply(InjectSource source) {
        source.add(Registry.COMPONENT_REGISTRY, this);
    }

    private static void startCluster(RegistryService registry) throws Exception {
        try {
            registry.start();
            logger.info("Success starting registry: {}", registry.getName());
        } catch (Exception e) {
            logger.error("Failed to start registry: {}", registry.getName(), e);
            throw e;
        }
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
        registrations.clear();
    }

    /**
     * Creates a new {@link Registration} object based on the provided service instance and registration action.
     * If the registration action ({@code doRegister}) is null, the existing registries are used.
     * If the registries list is null, a new list is created with the provided registration action and existing registries.
     *
     * @param instance   The service instance to be registered.
     * @param doRegister A {@link Callable} representing the registration action. If null, the existing registries are used.
     * @return A new {@link Registration} object containing the service instance, registries, publisher, registry configuration, and timer.
     */
    private Registration createRegistration(ServiceInstance instance, Callable<Void> doRegister) {
        // violate
        List<RegistryService> services = registries;
        List<ClusterRegistry> result = new ArrayList<>(services == null ? 1 : services.size() + 1);
        if (doRegister != null) {
            result.add(new ClusterRegistry(new FrameworkRegistryService(doRegister)));
        }
        if (services != null) {
            for (RegistryService service : services) {
                result.add(new ClusterRegistry(service));
            }
        }
        return new Registration(instance, result, timer);
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

        private final List<ClusterRegistry> clusters;

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

        Registration(ServiceInstance instance,
                     List<ClusterRegistry> clusters,
                     Timer timer) {
            this.clusters = clusters;
            this.service = instance.getService();
            this.instance = instance;
            this.timer = timer;
        }

        /**
         * Registers the service instance with the registry.
         */
        public void register() {
            if (registered.compareAndSet(false, true)) {
                if (clusters != null && !clusters.isEmpty()) {
                    doRegister();
                } else {
                    throw new RegistryException("Registry center is not configured");
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
                int counter = 0;
                for (ClusterRegistry cluster : clusters) {
                    if (!cluster.isRegistered()) {
                        String group = cluster.getGroup(instance.getGroup());
                        try {
                            cluster.register(instance.getService(), group, instance);
                            logger.info("Success registering instance {}:{} to {}@{} at {}",
                                    instance.getHost(), instance.getPort(),
                                    instance.getService(), group, cluster);
                            counter++;
                        } catch (Exception e) {
                            logger.error("Failed to register instance {}:{} to {}@{} at {}, caused by {}",
                                    instance.getHost(), instance.getPort(),
                                    instance.getService(), group, cluster, e.getMessage(), e);
                        }
                    } else {
                        counter++;
                    }
                }
                if (counter != clusters.size()) {
                    delayRegister();
                }
            }
        }

        /**
         * Performs the actual unregister of the service instance.
         */
        private void doUnregister() {
            for (ClusterRegistry cluster : clusters) {
                if (cluster.isRegistered()) {
                    String group = cluster.getGroup(instance.getGroup());
                    try {
                        cluster.unregister(instance.getService(), group, instance);
                        logger.info("Success unregistering instance {}:{} to {}@{} at {}",
                                instance.getHost(), instance.getPort(),
                                instance.getService(), group, cluster);
                    } catch (Exception e) {
                        logger.error("Failed to unregister instance {}:{} to {}@{} at {}, caused by {}",
                                instance.getHost(), instance.getPort(),
                                instance.getService(), group, cluster, e.getMessage(), e);
                    }
                }
            }
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

    /**
     * Represents a registration of a service to a cluster, tracking the registration status and retry attempts.
     * This class is used to manage the lifecycle of a service registration within a cluster.
     */
    private static class ClusterRegistry {

        @Getter
        private final RegistryService cluster;

        private final AtomicBoolean registered = new AtomicBoolean(false);

        private final AtomicLong retry = new AtomicLong(0);

        ClusterRegistry(RegistryService cluster) {
            this.cluster = cluster;
        }

        public String getGroup(String defaultGroup) {
            RegistryClusterConfig config = cluster.getConfig();
            return config == null ? defaultGroup : config.getGroup(defaultGroup);
        }

        public boolean isRegistered() {
            return registered.get();
        }

        public void setRegistered(boolean registered) {
            this.registered.set(registered);
        }

        public long getRetry() {
            return retry.get();
        }

        public void addRetry() {
            retry.incrementAndGet();
        }

        /**
         * Registers a service instance with the registry.
         *
         * @param instance The service instance to be registered.
         */
        public void register(String service, String group, ServiceInstance instance) throws Exception {
            cluster.register(service, group, instance);
            registered.set(true);
        }

        /**
         * Unregisters a service instance from the registry.
         *
         * @param instance The service instance to be unregistered.
         */
        public void unregister(String service, String group, ServiceInstance instance) throws Exception {
            cluster.unregister(service, group, instance);
            registered.set(false);
        }
    }

    /**
     * A specialized implementation of {@link AbstractRegistryService} that performs registration
     * using a provided callback. This class is designed to handle framework-specific registration logic.
     */
    private static class FrameworkRegistryService extends AbstractRegistryService {

        private final Callable<Void> callback;

        FrameworkRegistryService(Callable<Void> callback) {
            this.callback = callback;
        }

        @Override
        public String getName() {
            return "framework";
        }

        @Override
        public void register(String service, String group, ServiceInstance instance) throws Exception {
            callback.call();
        }
    }
}

