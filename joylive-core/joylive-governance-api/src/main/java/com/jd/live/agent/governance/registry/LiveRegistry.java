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
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.map.CaseInsensitiveConcurrentMap;
import com.jd.live.agent.core.util.shutdown.GracefullyShutdown;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.*;
import com.jd.live.agent.governance.counter.FlyingCounter;
import com.jd.live.agent.governance.exception.RegistryException;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.jd.live.agent.core.Constants.SAME_GROUP_PREDICATE;
import static com.jd.live.agent.core.util.StringUtils.choose;

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
public class LiveRegistry extends AbstractService
        implements CompositeRegistry, InjectSourceSupplier, AppListenerSupplier, ExtensionInitializer, GracefullyShutdown {

    private static final Logger logger = LoggerFactory.getLogger(LiveRegistry.class);

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    @Inject
    private Map<String, RegistryFactory> factories;

    private FlyingCounter flyingCounter;

    private RegistryConfig registryConfig;

    private ServiceConfig serviceConfig;

    private volatile List<RegistryService> registries;

    private final Set<RegistryService> systemRegistries = new CopyOnWriteArraySet<>();

    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    private final Map<String, Set<RegistryService>> serviceSystemRegistries = new ConcurrentHashMap<>();

    // fix for eureka
    private final Map<String, Registration> registrations = new CaseInsensitiveConcurrentMap<>();

    // fix for eureka
    private final Map<String, Subscription> subscriptions = new CaseInsensitiveConcurrentMap<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @Override
    public void initialize() {
        flyingCounter = policySupplier.getCounterManager().getFlyingCounter();
        serviceConfig = governanceConfig.getServiceConfig();
        registryConfig = governanceConfig.getRegistryConfig();
    }

    @Override
    public int getWaitTime() {
        return governanceConfig.getShutdownWaitTime();
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        // start registries
        List<RegistryClusterConfig> clusters = registryConfig.getClusters();
        List<RegistryService> registries = new ArrayList<>();
        try {
            if (clusters != null && registryConfig.isEnabled()) {
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
            if (!registries.isEmpty()) {
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
        CompletableFuture<Void> future = new CompletableFuture<>();
        ready.set(false);
        logger.info("Unregister services from registry.");
        // unregister first
        for (Registration registration : registrations.values()) {
            registration.unregister();
        }
        logger.info("Wait for completing flying requests.");
        // wait for flying request done
        flyingCounter.waitDone().whenComplete((v, e) -> {
            logger.info("Complete flying requests, start closing registry.");
            // stop and clear registrations
            for (Registration registration : registrations.values()) {
                registration.stop();
            }
            registrations.clear();
            // stop and clear subscriptions
            for (Subscription subscription : subscriptions.values()) {
                subscription.stop();
            }
            subscriptions.clear();
            // stop registries
            Close.instance().close(registries);
            registries = null;
            future.complete(null);
        });

        return future;
    }

    @Override
    public void setServiceAlias(String alias, String name) {
        if (alias != null && name != null) {
            aliases.putIfAbsent(alias, name);
        }
    }

    @Override
    public AppListener getAppListener() {
        return new AppListener.AppListenerAdapter() {

            @Override
            public void onReady(AppContext context) {
                onApplicationReady();
            }
        };
    }

    @Override
    public void addSystemRegistry(RegistryService registryService) {
        if (registryService != null && systemRegistries.add(registryService)) {
            subscriptions.forEach((key, value) -> value.addCluster(registryService));
        }
    }

    @Override
    public void removeSystemRegistry(RegistryService registryService) {
        if (registryService != null && systemRegistries.remove(registryService)) {
            subscriptions.forEach((key, value) -> value.removeCluster(registryService));
        }
    }

    @Override
    public void addSystemRegistry(String service, RegistryService registryService) {
        // for spring simple discovery client
        if (registryService != null && service != null
                && serviceSystemRegistries.computeIfAbsent(service, k -> new CopyOnWriteArraySet<>()).add(registryService)) {
            subscriptions.forEach((key, value) -> value.addCluster(registryService, service));
        }
    }

    @Override
    public void register(List<ServiceInstance> instances, Callable<Void> doRegister) {
        if (instances == null || instances.isEmpty()) {
            return;
        }
        for (ServiceInstance instance : instances) {
            customize(instance);
        }
        ServiceInstance instance = instances.get(0);
        // CaseInsensitiveConcurrentHashMap
        // Multi-registry
        Registration registration = registrations.computeIfAbsent(instance.getUniqueName(), n -> createRegistration(instances, doRegister));
        if (ready.get()) {
            registration.register();
        } else {
            // delay register
            instances.forEach(i -> logger.info("Delay registering instance {}:{} to {} until application is ready",
                    i.getHost(), i.getPort(), i.getService()));
        }
    }

    @Override
    public void unregister(ServiceInstance instance) {
        if (instance == null) {
            return;
        }
        customize(instance);
        Registration registration = registrations.remove(instance.getUniqueName());
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public CompletableFuture<Void> register(ServiceId serviceId) {
        serviceId = customize(serviceId, ServiceRole.PROVIDER);
        if (serviceId == null) {
            return CompletableFuture.completedFuture(null);
        }
        return policySupplier.subscribe(serviceId.getService());
    }

    @Override
    public CompletableFuture<Void> subscribe(ServiceId serviceId) {
        serviceId = customize(serviceId, ServiceRole.CONSUMER);
        if (serviceId == null) {
            return CompletableFuture.completedFuture(null);
        }
        // subscribe instance
        doSubscribe(serviceId, null);
        // subscribe govern policy
        return policySupplier.subscribe(serviceId.getService());
    }

    @Override
    public void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) {
        serviceId = customize(serviceId, ServiceRole.CONSUMER);
        if (serviceId != null) {
            doSubscribe(serviceId, consumer);
        }
    }

    @Override
    public void unsubscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) {
        serviceId = customize(serviceId, ServiceRole.CONSUMER);
        Subscription subscription = serviceId == null ? null : subscriptions.get(serviceId.getUniqueName());
        if (subscription != null) {
            subscription.removeConsumer(consumer);
        }
    }

    @Override
    public boolean isSubscribed(ServiceId serviceId) {
        serviceId = customize(serviceId, ServiceRole.CONSUMER);
        return serviceId != null && subscriptions.containsKey(serviceId.getUniqueName());
    }

    @Override
    public boolean isReady(ServiceId serviceId) {
        serviceId = customize(serviceId, ServiceRole.CONSUMER);
        return serviceId != null && policySupplier.isReady(serviceId.getNamespace(), serviceId.getService());
    }

    @Override
    public CompletionStage<List<ServiceEndpoint>> getEndpoints(String service, String group, ServiceRegistryFactory system) {
        ServiceRegistry registry;
        switch (registryConfig.getSubscribeMode()) {
            case LIVE:
                // convert service name in getServiceRegistry
                registry = getServiceRegistry(service, group);
                return registry == null ? CompletableFuture.completedFuture(new ArrayList<>()) : registry.getEndpoints();
            case SYSTEM:
                // don't convert service name in system registry
                return system.getEndpoints(service);
            case AUTO:
            default:
                registry = getServiceRegistry(service, group);
                if (system == null) {
                    return registry == null ? CompletableFuture.completedFuture(new ArrayList<>()) : registry.getEndpoints();
                } else if (registry == null) {
                    return system.getEndpoints(service);
                } else {
                    CompletableFuture<List<ServiceEndpoint>> future = new CompletableFuture<>();
                    registry.getEndpoints().whenComplete((v, throwable) -> {
                        if (throwable == null && v != null && !v.isEmpty()) {
                            future.complete(v);
                        } else {
                            // don't convert service name in system registry
                            system.getEndpoints(service).whenComplete((e, r) -> {
                                if (r != null) {
                                    future.completeExceptionally(r);
                                } else {
                                    future.complete(e);
                                }
                            });
                        }
                    });
                    return future;
                }
        }
    }

    @Override
    public ServiceRegistry getServiceRegistry(ServiceId serviceId) {
        serviceId = customize(serviceId, ServiceRole.CONSUMER);
        return serviceId == null ? null : subscriptions.get(serviceId.getUniqueName());
    }

    @Override
    public void apply(InjectSource source) {
        source.add(Registry.COMPONENT_REGISTRY, this);
    }

    /**
     * Updates service instance metadata if service/group mappings exist.
     * Resets unique name if any changes were made.
     */
    private void customize(ServiceInstance instance) {
        ServiceId serviceId = customize(instance, ServiceRole.PROVIDER);
        if (serviceId == null || serviceId == instance) {
            // service is empty or unmodified;
            return;
        }
        if (!serviceId.getService().equalsIgnoreCase(instance.getService())) {
            instance.setService(serviceId.getService());
            instance.setUniqueName(null);
        }
        if (!SAME_GROUP_PREDICATE.test(serviceId.getGroup(), instance.getGroup())) {
            instance.setGroup(serviceId.getGroup());
            instance.setUniqueName(null);
        }
    }

    /**
     * Resolves and normalizes service identification information from a ServiceId.
     *
     * @param serviceId the ServiceId containing raw service identification data
     * @param role the service role ({@link ServiceRole#CONSUMER} or {@link ServiceRole#PROVIDER})
     *             determining resolution strategy for empty groups
     * @return normalized {@code ServiceId} containing canonical names, or:
     */
    private ServiceId customize(ServiceId serviceId, ServiceRole role) {
        if (serviceId == null) {
            return null;
        }
        AppService appService = application.getService();
        String service = choose(serviceId.getService(), appService.getName());
        final String target = choose(aliases.get(service), service);
        String group = choose(serviceId.getGroup(), () -> role == ServiceRole.CONSUMER ? serviceConfig.getGroup(target) : appService.getGroup());
        return serviceId.of(target, group);
    }

    /**
     * Subscribes a consumer to registry events for the specified service.
     *
     * @param serviceId the service identifier to subscribe to
     * @param consumer  callback to receive registry events
     * @see Subscription
     * @see RegistryEvent
     */
    private void doSubscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) {
        // CaseInsensitiveConcurrentHashMap
        Subscription subscription = subscriptions.computeIfAbsent(serviceId.getUniqueName(), s -> createSubscription(serviceId));
        subscription.addConsumer(consumer);
        subscription.subscribe();
    }

    /**
     * Starts registry cluster instance, logging success/failure.
     *
     * @param registry the registry service instance to start
     * @throws Exception if startup fails
     */
    private void startCluster(RegistryService registry) throws Exception {
        try {
            registry.start();
            logger.info("Success starting registry: {}", registry.getDescription());
        } catch (Exception e) {
            logger.error("Failed to start registry: {}", registry.getDescription(), e);
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
     * Creates a new {@link Registration} object based on the provided service instance and registration action.
     *
     * @param instances   The service instances to be registered.
     * @param doRegister A {@link Callable} representing the registration action. If null, the existing registries are used.
     * @return A new {@link Registration} object containing the service instance, registries, publisher, registry configuration, and timer.
     */
    private Registration createRegistration(List<ServiceInstance> instances, Callable<Void> doRegister) {
        // violate
        List<RegistryService> clusters = registries;
        List<ClusterInstanceRegistration> values = new CopyOnWriteArrayList<>();
        if (doRegister != null) {
            RegistryService cluster;
            for (ServiceInstance instance : instances) {
                cluster = doRegister instanceof RegistryCallable
                        ? ((RegistryCallable<?>) doRegister).getRegistry()
                        : new SystemRegistryService();
                values.add(new ClusterInstanceRegistration(cluster, instance, doRegister));
            }
        }
        if (clusters != null) {
            // TODO Fix multi registry issue.
            // Application register service in nacos for gateway and consume other service in zookeepers
            for (RegistryService cluster : clusters) {
                if (cluster.getConfig().getMode().isRegister()) {
                    for (ServiceInstance instance : instances) {
                        values.add(new ClusterInstanceRegistration(cluster, instance));
                    }
                }
            }
        }
        return new Registration(name, values, timer);
    }

    /**
     * Creates a subscription for the specified service and group by initializing a list of {@link ClusterOperation} instances
     * from the available {@link RegistryService} clusters. The subscription is then constructed with the service, group,
     * cluster registries, and a timer.
     *
     * @param serviceId the service id to subscribe to.
     * @return a new {@link Subscription} instance containing the service, group, cluster registries, and timer.
     */
    private Subscription createSubscription(ServiceId serviceId) {
        // violate
        List<RegistryService> clusters = registries;
        List<ClusterSubscription> values = new ArrayList<>(clusters == null ? 0 : clusters.size());
        if (clusters != null) {
            for (RegistryService cluster : clusters) {
                values.add(new ClusterSubscription(cluster, serviceId));
            }
        }
        systemRegistries.forEach(registry -> values.add(new ClusterSubscription(registry, serviceId)));
        // for spring simple discovery client
        Set<RegistryService> systems = serviceSystemRegistries.get(serviceId.getService());
        if (systems != null) {
            systems.forEach(registry -> values.add(new ClusterSubscription(registry, serviceId)));
        }
        sort(values);
        return new Subscription(serviceId, values, timer);
    }

    /**
     * Sorts cluster subscriptions by their registry role in descending priority order:
     * <ol>
     *   <li>PRIMARY</li>
     *   <li>SYSTEM</li>
     *   <li>SECONDARY (default for null roles)</li>
     * </ol>
     *
     * @param subscriptions the list of cluster subscriptions to be sorted in-place
     */
    private static void sort(List<ClusterSubscription> subscriptions) {
        // sort by role PRIMARY > SYSTEM > SECONDARY
        subscriptions.sort((o1, o2) -> {
            RegistryRole role1 = o1.getConfig().getRole();
            RegistryRole role2 = o2.getConfig().getRole();
            role1 = role1 == null ? RegistryRole.SECONDARY : role1;
            role2 = role2 == null ? RegistryRole.SECONDARY : role2;
            return role1.getOrder() - role2.getOrder();
        });
    }

    /**
     * A private static class that represents a registration of a service instance with the registry.
     */
    private static class Registration {

        /**
         * The name of the registration.
         */
        private final String name;

        /**
         * The service instance being registered.
         */
        private final List<ClusterInstanceRegistration> instances;

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

        Registration(String name, List<ClusterInstanceRegistration> instances, Timer timer) {
            this.name = name;
            this.instances = instances;
            this.timer = timer;
        }

        /**
         * Registers the service instance with the registry.
         */
        public void register() {
            if (registered.compareAndSet(false, true)) {
                if (instances != null && !instances.isEmpty()) {
                    doRegister();
                } else {
                    throw new RegistryException("Registry center is not configured");
                }
            }
        }

        public void register(ServiceInstance instance) {
            for (ClusterInstanceRegistration registration : instances) {
                if (!(registration.getCluster() instanceof AbstractSystemRegistryService)
                        && Objects.equals(registration.getInstance().getSchemeAddress(), instance.getSchemeAddress())) {
                    registration.register();
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

        public void unregister(ServiceInstance instance) {
            for (ClusterInstanceRegistration registration : instances) {
                if (!(registration.getCluster() instanceof AbstractSystemRegistryService)
                        && Objects.equals(registration.getInstance().getSchemeAddress(), instance.getSchemeAddress())) {
                    registration.unregister();
                }
            }
        }

        /**
         * Stops the registration process.
         */
        public void stop() {
            started.set(false);
            unregister();
        }

        public void add(Callable<Void> doRegister) {

        }

        /**
         * Delays the register process by a random amount of time.
         */
        private void delayRegister() {
            long delay = 1000 + (long) (Math.random() * 2000.0);
            timer.delay("register-" + name, delay, this::doRegister);
        }

        /**
         * Performs the actual register of the service instance.
         */
        private void doRegister() {
            if (!started.get()) {
                return;
            }
            int dones = 0;
            for (ClusterInstanceRegistration registration : instances) {
                if (registration.register()) {
                    dones++;
                }
            }
            if (dones < instances.size()) {
                delayRegister();
            }
        }

        /**
         * Performs the actual unregister of the service instance.
         */
        private void doUnregister() {
            for (ClusterInstanceRegistration registration : instances) {
                registration.unregister();
            }
        }
    }

    /**
     * A private static class that represents a subscription to endpoint events for a specific service group.
     */
    private static class Subscription implements ServiceRegistry {

        private static final int UNSUBSCRIBE = 0;

        private static final int SUBSCRIBING = 1;

        private static final int SUBSCRIBED = 2;

        /**
         * The service group that this subscription is for.
         */
        @Getter
        private final ServiceId serviceId;

        private volatile List<ClusterSubscription> clusters;

        /**
         * A timer used to schedule heartbeat and registration delays.
         */
        private final Timer timer;

        /**
         * An atomic boolean indicating whether the registration has been started.
         */
        private final AtomicBoolean started = new AtomicBoolean(true);

        /**
         * An atomic integer indicating whether the registration has been completed.
         */
        private final AtomicInteger subscribed = new AtomicInteger(UNSUBSCRIBE);

        /**
         * The consumer that will receive endpoint events.
         */
        private final List<Consumer<RegistryEvent>> consumers = new CopyOnWriteArrayList<>();

        private final Map<String, List<ServiceEndpoint>> clustersEndpoints = new ConcurrentHashMap<>();

        /**
         * A map of endpoints for the service group, keyed by their addresses.
         */
        private volatile RegistryEvent event;

        private final AtomicBoolean notified = new AtomicBoolean(false);

        private final Object mutex = new Object();

        Subscription(ServiceId service, List<ClusterSubscription> clusters, Timer timer) {
            this.serviceId = service;
            this.clusters = clusters;
            this.timer = timer;
        }

        @Override
        public String getService() {
            return serviceId.getService();
        }

        @Override
        public CompletableFuture<List<ServiceEndpoint>> getEndpoints() {
            RegistryEvent event = this.event;
            return CompletableFuture.completedFuture(event == null ? null : event.getInstances());
        }

        public void addCluster(RegistryService cluster, String service) {
            if (serviceId.getService().equals(service)) {
                addCluster(cluster);
            }
        }

        public void addCluster(RegistryService cluster) {
            synchronized (mutex) {
                List<ClusterSubscription> clusters = new ArrayList<>(this.clusters);
                clusters.add(new ClusterSubscription(cluster, serviceId));
                sort(clusters);
                this.clusters = clusters;
            }
            resubscribe();
        }

        public void removeCluster(RegistryService cluster) {
            boolean removed = false;
            synchronized (mutex) {
                List<ClusterSubscription> clusters = new ArrayList<>(this.clusters);
                for (int i = clusters.size() - 1; i >= 0; i--) {
                    if (clusters.get(i).getCluster() == cluster) {
                        clusters.remove(i);
                        removed = true;
                        break;
                    }
                }
                if (removed) {
                    sort(clusters);
                    this.clusters = clusters;
                }
            }
            if (removed) {
                resubscribe();
            }
        }

        public void removeCluster(RegistryService cluster, String service) {
            if (serviceId.getService().equals(service)) {
                removeCluster(cluster);
            }
        }

        /**
         * Adds a new consumer to the list of consumers that will receive endpoint events.
         *
         * @param consumer the consumer to add
         */
        public void addConsumer(Consumer<RegistryEvent> consumer) {
            if (consumer != null) {
                synchronized (mutex) {
                    if (!consumers.contains(consumer)) {
                        consumers.add(consumer);
                        RegistryEvent event = this.event;
                        if (event != null) {
                            consumer.accept(event);
                        }
                    }
                }
            }
        }

        /**
         * Removes an event consumer from subscription.
         *
         * @param consumer the consumer to remove (nullable)
         */
        public void removeConsumer(Consumer<RegistryEvent> consumer) {
            if (consumer != null) {
                synchronized (mutex) {
                    consumers.remove(consumer);
                }
            }
        }

        /**
         * Subscribes to the service if it has not already been subscribed.
         */
        public void subscribe() {
            if (subscribed.compareAndSet(UNSUBSCRIBE, SUBSCRIBING)) {
                doSubscribe();
            } else if (subscribed.get() == SUBSCRIBED) {
                resubscribe();
            }
        }

        /**
         * Stops the registration process.
         */
        public void stop() {
            started.set(false);
            doUnsubscribe();
        }

        /**
         * Checks if the event matches the service criteria.
         *
         * @param event registry event to match
         * @return true if event matches service criteria
         */
        private boolean match(RegistryEvent event) {
            return serviceId.match(event.getServiceId(), event.getDefaultGroup());
        }

        /**
         * Updates service endpoints for a cluster based on registry changes.
         *
         * @param clusterName the target cluster name
         * @param event registry update event (null-safe, zero size removes endpoints)
         */
        private void update(String clusterName, RegistryEvent event) {
            if (!started.get() || event == null || !match(event)) {
                return;
            }
            synchronized (mutex) {
                if (!started.get()) {
                    return;
                }
                event = delta(clusterName, event);
                int newSize = event.size();

                List<ServiceEndpoint> olds = newSize == 0 ? clustersEndpoints.remove(clusterName) : clustersEndpoints.put(clusterName, event.getInstances());
                int oldSize = olds == null ? 0 : olds.size();
                if (newSize == 0 && oldSize == 0 && olds != null) {
                    // ignore duplicated empty event
                    return;
                }

                // merge endpoints by order
                RegistryEvent oldsEvent = this.event;
                int capacity = oldsEvent == null ? 0 : oldsEvent.size();
                capacity = capacity + newSize - oldSize;
                Map<String, ServiceEndpoint> merged = new HashMap<>(capacity);
                for (ClusterSubscription cluster : clusters) {
                    olds = clustersEndpoints.get(cluster.getClusterName());
                    if (olds != null) {
                        olds.forEach(endpoint -> merged.putIfAbsent(endpoint.getAddress(), endpoint));
                    }
                }
                List<ServiceEndpoint> newEndpoints = new ArrayList<>(merged.values());

                // log
                StringBuilder builder = new StringBuilder("Merge instances to ").append(newEndpoints.size()).append(" [")
                        .append(oldSize).append("->").append(newSize).append(" in ").append(clusterName);
                clustersEndpoints.forEach((name, endpoints) -> {
                    if (!name.equals(clusterName)) {
                        builder.append(", ").append(endpoints.size()).append(" in ").append(name);
                    }
                });
                builder.append("], ").append(serviceId.getUniqueName());
                logger.info(builder.toString());

                this.event = new RegistryEvent(event.getVersion(), serviceId.getService(), serviceId.group, newEndpoints, event.getDefaultGroup());
                addNotifier();
            }
        }

        /**
         * Schedules a delayed notification to all consumers if no notification is currently active.
         * Uses atomic check to prevent duplicate notifications. If the event changes during processing,
         * automatically re-triggers notification.
         */
        private void addNotifier() {
            if (notified.compareAndSet(false, true)) {
                timer.delay("instance-notifier", 1000, () -> {
                    RegistryEvent e = event;
                    try {
                        // TODO duplicated consumers
                        for (Consumer<RegistryEvent> consumer : consumers) {
                            consumer.accept(new RegistryEvent(serviceId.getService(), serviceId.group, e.getInstances()));
                        }
                    } finally {
                        notified.set(false);
                        if (event != e) {
                            addNotifier();
                        }
                    }
                });
            }
        }

        /**
         * Applies delta changes to service endpoints for a cluster.
         * Handles FULL/ADD/UPDATE/REMOVE operations from delta events.
         *
         * @param clusterName target cluster name
         * @param event registry change event
         * @return updated registry event
         */
        private RegistryEvent delta(String clusterName, RegistryEvent event) {
            if (event instanceof RegistryDeltaEvent) {
                RegistryDeltaEvent deltaEvent = (RegistryDeltaEvent) event;
                switch (deltaEvent.getType()) {
                    case FULL:
                        break;
                    case REMOVE:
                        return delta(clusterName, deltaEvent, (map, instance) -> map.remove(instance.getAddress()));
                    case ADD:
                    case UPDATE:
                    default:
                        return delta(clusterName, deltaEvent, (map, instance) -> map.put(instance.getAddress(), instance));
                }
            }
            return event;
        }

        /**
         * Merges endpoint changes using the provided update operation.
         *
         * @param clusterName target cluster name
         * @param event registry change event
         * @param consumer operation to apply (add/remove endpoints)
         * @return new registry event with merged endpoints
         */
        private RegistryEvent delta(String clusterName, RegistryEvent event, BiConsumer<Map<String, ServiceEndpoint>, ServiceEndpoint> consumer) {
            List<ServiceEndpoint> oldEndpoints = clustersEndpoints.get(clusterName);
            Map<String, ServiceEndpoint> merged = new HashMap<>(oldEndpoints == null ? 0 : oldEndpoints.size());
            if (oldEndpoints != null) {
                oldEndpoints.forEach(endpoint -> merged.putIfAbsent(endpoint.getAddress(), endpoint));
            }
            event.getInstances().forEach(instance -> consumer.accept(merged, instance));
            return new RegistryEvent(event.getServiceId(), new ArrayList<>(merged.values()), event.getDefaultGroup());
        }

        /**
         * Performs the actual subscription of the service.
         */
        private void doSubscribe() {
            if (!started.get()) {
                return;
            }
            int dones = 0;
            for (ClusterSubscription cluster : clusters) {
                if (cluster.subscribe(e -> update(cluster.getClusterName(), e))) {
                    dones++;
                }
            }
            if (dones != clusters.size()) {
                delaySubscribe();
            } else {
                subscribed.set(SUBSCRIBED);
                resubscribe();
            }
        }

        /**
         * Delays the register process by a random amount of time.
         */
        private void delaySubscribe() {
            long delay = 1000 + (long) (Math.random() * 2000.0);
            timer.delay("subscribe-" + serviceId, delay, this::doSubscribe);
        }

        private void resubscribe() {
            for (ClusterSubscription cluster : clusters) {
                if (!cluster.isDone()) {
                    // Add new cluster subscription
                    if (subscribed.compareAndSet(SUBSCRIBED, SUBSCRIBING)) {
                        doSubscribe();
                    }
                    break;
                }
            }
        }

        /**
         * Performs the actual unsubscription of the service.
         */
        private void doUnsubscribe() {
            for (ClusterSubscription cluster : clusters) {
                if (!cluster.isDone()) {
                    cluster.unsubscribe();
                }
            }
        }
    }

    /**
     * Abstract base class for cluster operations. Provides common functionality for managing
     * operations on a {@link RegistryService} cluster.
     */
    private abstract static class ClusterOperation<T extends ServiceId> {

        @Getter
        protected final RegistryService cluster;

        @Getter
        protected final T instance;

        @Getter
        protected final ServiceId serviceId;

        @Getter
        protected final String name;

        protected final AtomicBoolean done = new AtomicBoolean(false);

        ClusterOperation(RegistryService cluster, T instance) {
            this.cluster = cluster;
            this.instance = instance;
            this.serviceId = new ServiceId(instance.getService(), getClusterGroup(cluster, instance.group), instance.isInterfaceMode());
            this.name = instance.getUniqueName();
        }

        public String getClusterName() {
            return cluster.getName();
        }

        public boolean isDone() {
            return done.get();
        }

        public void setDone(boolean done) {
            this.done.set(done);
        }

        public RegistryClusterConfig getConfig() {
            return cluster.getConfig();
        }

        protected String getClusterGroup(RegistryService cluster, String defaultGroup) {
            RegistryClusterConfig config = cluster.getConfig();
            return config == null ? defaultGroup : config.getGroup(defaultGroup);
        }

    }

    private static class ClusterInstanceRegistration extends ClusterOperation<ServiceInstance> {

        private final Callable<Void> doRegister;

        ClusterInstanceRegistration(RegistryService cluster, ServiceInstance instance) {
            this(cluster, instance, null);
        }

        ClusterInstanceRegistration(RegistryService cluster, ServiceInstance instance, Callable<Void> doRegister) {
            super(cluster, instance);
            this.doRegister = doRegister;
        }

        /**
         * Registers a service instance with the registry.
         */
        public boolean register() {
            if (!isDone()) {
                try {
                    if (doRegister != null) {
                        doRegister.call();
                    } else {
                        cluster.register(serviceId, instance);
                    }
                    setDone(true);
                    logger.info("Success registering instance {} to {} at {}",
                            instance.getSchemeAddress(), name, cluster.getName());
                    return true;
                } catch (Throwable e) {
                    logger.error("Failed to register instance {} to {} at {}, caused by {}",
                            instance.getSchemeAddress(), name, cluster.getName(), e.getMessage(), e);
                }
                return false;
            }
            return true;
        }

        /**
         * Unregisters a service instance from the registry.
         */
        public void unregister() {
            if (isDone()) {
                try {
                    cluster.unregister(serviceId, instance);
                    setDone(false);
                    logger.info("Success unregistering instance {} to {} at {}",
                            instance.getSchemeAddress(), name, cluster.getName());
                } catch (Exception e) {
                    logger.error("Failed to unregister instance {} to {} at {}, caused by {}",
                            instance.getSchemeAddress(), name, cluster.getName(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Handles subscription and unsubscription to endpoint events for a {@link RegistryService} cluster.
     * Extends {@link ClusterOperation} to manage event listening for specific services.
     */
    private static class ClusterSubscription extends ClusterOperation<ServiceId> {

        ClusterSubscription(RegistryService cluster, ServiceId service) {
            super(cluster, service);
        }

        /**
         * Subscribes to endpoint events for a specific service and group.
         *
         * @param consumer The consumer to handle endpoint events.
         */
        public boolean subscribe(Consumer<RegistryEvent> consumer) {
            if (isDone()) {
                return true;
            }
            try {
                cluster.subscribe(serviceId, consumer);
                setDone(true);
                logger.info("Success subscribing {} at {}", name, cluster.getName());
                return true;
            } catch (Exception e) {
                logger.error("Failed to subscribe {} at {}, caused by {}", name, cluster.getName(), e.getMessage(), e);
            }
            return false;
        }

        /**
         * Unsubscribes from endpoint events for a specific service and group.
         *
         */
        public void unsubscribe() {
            try {
                cluster.unsubscribe(serviceId);
                setDone(false);
                logger.info("Success unsubscribing {} at {}", name, getClusterName());
            } catch (Exception e) {
                logger.error("Failed to unsubscribe {} at {}, caused by {}", name, getClusterName(), e.getMessage(), e);
            }
        }
    }

    /**
     * A specialized implementation of {@link AbstractRegistryService} that performs registration
     * using a provided callback. This class is designed to handle framework-specific registration logic.
     */
    private static class SystemRegistryService extends AbstractSystemRegistryService {

        private final Callable<Void> callback;

        SystemRegistryService() {
            this(null);
        }

        SystemRegistryService(Callable<Void> callback) {
            super(SYSTEM);
            this.callback = callback;
        }

        @Override
        public void register(ServiceId serviceId, ServiceInstance instance) throws Exception {
            if (callback != null) {
                callback.call();
            }
        }

        @Override
        protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) throws Exception {
            return null;
        }

        @Override
        protected RegistryClusterConfig createDefaultConfig() {
            return new RegistryClusterConfig(RegistryRole.SYSTEM, RegistryMode.REGISTER);
        }
    }
}

