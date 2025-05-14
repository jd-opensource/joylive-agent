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
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.map.CaseInsensitiveConcurrentMap;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.*;
import com.jd.live.agent.governance.exception.RegistryException;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
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
public class LiveRegistry extends AbstractService implements CompositeRegistry, InjectSourceSupplier, AppListenerSupplier {

    private static final Logger logger = LoggerFactory.getLogger(LiveRegistry.class);

    @Inject(RegistryConfig.COMPONENT_REGISTRY_CONFIG)
    private RegistryConfig registryConfig;

    @Inject(RegistryConfig.COMPONENT_REGISTRY_CONFIG)
    private ServiceConfig serviceConfig;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    @Inject
    private Map<String, RegistryFactory> factories;

    private volatile List<RegistryService> registries;

    private RegistryService systemRegistry;

    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    private final Map<String, RegistryService> systemRegistries = new ConcurrentHashMap<>();

    // fix for eureka
    private final Map<String, Registration> registrations = new CaseInsensitiveConcurrentMap<>();

    // fix for eureka
    private final Map<String, Subscription> subscriptions = new CaseInsensitiveConcurrentMap<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

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
        ready.set(false);
        onApplicationStop();
        // stop registries
        Close.instance().close(registries);
        registries = null;
        return CompletableFuture.completedFuture(null);
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

            public void onStop(AppContext context) {
                onApplicationStop();
            }
        };
    }

    @Override
    public void setSystemRegistry(RegistryService registryService) {
        this.systemRegistry = registryService;
    }

    @Override
    public RegistryService getSystemRegistry() {
        return systemRegistry;
    }

    @Override
    public void addSystemRegistry(String service, RegistryService registryService) {
        if (service != null && registryService != null) {
            systemRegistries.put(service, registryService);
        }
    }

    @Override
    public RegistryService getSystemRegistry(String service) {
        return service == null ? null : systemRegistries.get(service);
    }

    @Override
    public void register(List<ServiceInstance> instances, Callable<Void> doRegister) {
        if (instances == null || instances.isEmpty()) {
            return;
        }
        for (ServiceInstance instance : instances) {
            update(instance);
        }
        ServiceInstance instance = instances.get(0);
        // CaseInsensitiveConcurrentHashMap
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
        update(instance);
        Registration registration = registrations.remove(instance.getUniqueName());
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public CompletableFuture<Void> register(String service, String group) {
        ServiceId serviceId = getServiceId(service, group, ServiceRole.PROVIDER);
        if (serviceId == null) {
            return CompletableFuture.completedFuture(null);
        }
        return policySupplier.subscribe(serviceId.getService());
    }

    @Override
    public CompletableFuture<Void> subscribe(String service, String group) {
        ServiceId serviceId = getServiceId(service, group, ServiceRole.CONSUMER);
        if (serviceId == null) {
            return CompletableFuture.completedFuture(null);
        }
        // subscribe instance
        doSubscribe(serviceId, null);
        // subscribe govern policy
        return policySupplier.subscribe(service);
    }

    @Override
    public void subscribe(String service, String group, Consumer<RegistryEvent> consumer) {
        ServiceId serviceId = getServiceId(service, group, ServiceRole.CONSUMER);
        if (serviceId == null) {
            return;
        }
        doSubscribe(serviceId, consumer);
    }

    @Override
    public boolean isSubscribed(String service, String group) {
        ServiceId serviceId = getServiceId(service, group, ServiceRole.CONSUMER);
        if (serviceId == null) {
            return false;
        }
        return subscriptions.containsKey(serviceId.getUniqueName());
    }

    @Override
    public boolean isReady(String namespace, String service) {
        ServiceId serviceId = getServiceId(service, null, ServiceRole.CONSUMER);
        if (serviceId == null) {
            return false;
        }
        return policySupplier.isReady(namespace, serviceId.getService());
    }

    @Override
    public CompletionStage<List<ServiceEndpoint>> getEndpoints(String service, String group, ServiceRegistryFactory system) {
        ServiceRegistry registry;
        switch (registryConfig.getSubscribeMode()) {
            case LIVE:
                // convert service name in getServiceRegistry
                registry = getServiceRegistry(service, group);
                return registry == null ? null : registry.getEndpoints();
            case SYSTEM:
                // don't convert service name in system registry
                return system.getEndpoints(service);
            case AUTO:
            default:
                registry = getServiceRegistry(service, group);
                if (system == null) {
                    return registry == null ? null : registry.getEndpoints();
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
    public ServiceRegistry getServiceRegistry(String service, String group) {
        ServiceId serviceId = getServiceId(service, group, ServiceRole.CONSUMER);
        if (serviceId == null) {
            return null;
        }
        return subscriptions.get(serviceId.getUniqueName());
    }

    @Override
    public void apply(InjectSource source) {
        source.add(Registry.COMPONENT_REGISTRY, this);
    }

    /**
     * Updates service instance metadata if service/group mappings exist.
     * Resets unique name if any changes were made.
     */
    private void update(ServiceInstance instance) {
        int count = 0;
        ServiceId serviceId = getServiceId(instance.getService(), instance.getGroup(), ServiceRole.PROVIDER);
        if (serviceId == null) {
            return;
        }
        String service = serviceId.getService();
        String group = serviceId.getGroup();
        if (service != null && !service.isEmpty() && !service.equals(instance.getService())) {
            instance.setService(service);
            count++;
        }
        if (group != null && !group.isEmpty() && !group.equals(instance.getGroup())) {
            instance.setGroup(group);
            count++;
        }
        if (count > 0) {
            // recreate unique name
            instance.setUniqueName(null);
        }
    }

    /**
     * Resolves and normalizes service identification information.
     *
     * <p>Performs the following normalization steps:
     * <ol>
     *   <li>If service name is empty/null, uses application's default service name</li>
     *   <li>If service name is still invalid (empty/null), returns null</li>
     *   <li>Looks up service name in aliases map (returns canonical name if alias exists)</li>
     *   <li>If group is empty/null, determines group based on role:
     *     <ul>
     *       <li>For consumers: gets group from service configuration</li>
     *       <li>For providers: uses application's default group</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param service the raw service name
     * @param group   the raw group name
     * @param role    whether the caller is a provider or consumer
     * @return normalized ServiceId containing canonical names, or null if service name is invalid
     * @see ServiceId
     */
    private ServiceId getServiceId(String service, String group, ServiceRole role) {
        if (service == null || service.isEmpty()) {
            service = application.getService().getName();
        }
        if (service == null || service.isEmpty()) {
            return null;
        }
        String alias = aliases.get(service);
        service = alias == null || alias.isEmpty() ? service : alias;
        if (group == null || group.isEmpty()) {
            group = role == ServiceRole.CONSUMER ? serviceConfig.getGroup(service) : application.getService().getGroup();
        }
        return new ServiceId(service, group);
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
     * Called when the application is stopping. This method iterates through all registered services and calls their stop method.
     */
    private void onApplicationStop() {
        ready.set(false);

        for (Registration registration : registrations.values()) {
            registration.stop();
        }
        registrations.clear();

        for (Subscription subscription : subscriptions.values()) {
            subscription.stop();
        }
        subscriptions.clear();
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
        List<ClusterInstanceRegistration> values = new ArrayList<>(clusters == null ? 1 : clusters.size() + 1);
        if (doRegister != null) {
            // SystemRegistryService, call doRegister
            values.add(new ClusterInstanceRegistration(new SystemRegistryService(doRegister), instances.get(0)));
        }
        if (clusters != null) {
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
     * @param service the service to subscribe to.
     * @return a new {@link Subscription} instance containing the service, group, cluster registries, and timer.
     */
    private Subscription createSubscription(ServiceId service) {
        // violate
        List<RegistryService> clusters = registries;
        List<ClusterSubscription> values = new ArrayList<>(clusters == null ? 0 : clusters.size());
        if (clusters != null) {
            for (RegistryService cluster : clusters) {
                values.add(new ClusterSubscription(cluster, service));
            }
        }
        if (systemRegistry != null) {
            values.add(new ClusterSubscription(systemRegistry, service));
        }
        // for spring simple discovery client
        RegistryService system = systemRegistries.get(service.getService());
        if (system != null) {
            values.add(new ClusterSubscription(system, service));
        }
        // sort by role PRIMARY > SYSTEM > SECONDARY
        values.sort((o1, o2) -> {
            RegistryRole role1 = o1.getConfig().getRole();
            RegistryRole role2 = o2.getConfig().getRole();
            role1 = role1 == null ? RegistryRole.SECONDARY : role1;
            role2 = role2 == null ? RegistryRole.SECONDARY : role2;
            return role1.getOrder() - role2.getOrder();
        });
        return new Subscription(service, values, timer);
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
            if (dones != instances.size()) {
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

        /**
         * The service group that this subscription is for.
         */
        @Getter
        private final ServiceId serviceId;

        private final List<ClusterSubscription> clusters;

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
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        /**
         * The consumer that will receive endpoint events.
         */
        private final List<Consumer<RegistryEvent>> consumers = new CopyOnWriteArrayList<>();

        private final Map<String, List<ServiceEndpoint>> clustersEndpoints = new ConcurrentHashMap<>();

        /**
         * A map of endpoints for the service group, keyed by their addresses.
         */
        private volatile List<ServiceEndpoint> endpoints;

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
            return CompletableFuture.completedFuture(endpoints);
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
                    }
                }
            }
        }

        /**
         * Subscribes to the service if it has not already been subscribed.
         */
        public void subscribe() {
            if (subscribed.compareAndSet(false, true)) {
                // allow clusters is empty.
                if (clusters != null && !clusters.isEmpty()) {
                    doSubscribe();
                }
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
         * Updates service endpoints for a cluster based on registry changes.
         *
         * @param clusterName the target cluster name
         * @param event registry update event (null-safe, zero size removes endpoints)
         */
        private void update(String clusterName, RegistryEvent event) {
            if (!started.get() || event == null) {
                return;
            }
            synchronized (mutex) {
                if (!started.get()) {
                    return;
                }
                event = delta(clusterName, event);
                int size = event.size();
                logger.info("Service instance count is changed to {}, {} at {}", size, serviceId.getUniqueName(), clusterName);

                List<ServiceEndpoint> olds = size == 0 ? clustersEndpoints.remove(clusterName) : clustersEndpoints.put(clusterName, event.getInstances());

                // merge endpoints by order
                int capacity = endpoints == null ? 0 : endpoints.size();
                capacity = capacity + size - (olds == null ? 0 : olds.size());
                Map<String, ServiceEndpoint> merged = new HashMap<>(capacity);
                for (ClusterSubscription cluster : clusters) {
                    olds = clustersEndpoints.get(cluster.getClusterName());
                    if (olds != null) {
                        olds.forEach(endpoint -> merged.putIfAbsent(endpoint.getAddress(), endpoint));
                    }
                }
                List<ServiceEndpoint> newEndpoints = new ArrayList<>(merged.values());
                this.endpoints = newEndpoints;
                for (Consumer<RegistryEvent> consumer : consumers) {
                    consumer.accept(new RegistryEvent(serviceId.getService(), serviceId.group, newEndpoints));
                }
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
            return new RegistryEvent(event.getService(), event.getGroup(), new ArrayList<>(merged.values()), event.getDefaultGroup());
        }

        /**
         * Delays the register process by a random amount of time.
         */
        private void delaySubscribe() {
            long delay = 1000 + (long) (Math.random() * 2000.0);
            timer.delay("subscribe-" + serviceId, delay, this::doSubscribe);
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
        protected final String service;

        @Getter
        protected final String group;

        @Getter
        protected final String name;

        protected final AtomicBoolean done = new AtomicBoolean(false);

        ClusterOperation(RegistryService cluster, T instance) {
            this.cluster = cluster;
            this.instance = instance;
            this.service = instance.getService();
            this.group = getClusterGroup(cluster, instance.group);
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

        ClusterInstanceRegistration(RegistryService cluster, ServiceInstance instance) {
            super(cluster, instance);
        }

        /**
         * Registers a service instance with the registry.
         */
        public boolean register() {
            if (!isDone()) {
                try {
                    cluster.register(service, group, instance);
                    setDone(true);
                    logger.info("Success registering instance {}:{} to {} at {}",
                            instance.getHost(), instance.getPort(), name, cluster.getName());
                    return true;
                } catch (Throwable e) {
                    logger.error("Failed to register instance {}:{} to {} at {}, caused by {}",
                            instance.getHost(), instance.getPort(), name, cluster.getName(), e.getMessage(), e);
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
                    cluster.unregister(service, group, instance);
                    setDone(false);
                    logger.info("Success unregistering instance {}:{} to {} at {}",
                            instance.getHost(), instance.getPort(), name, cluster.getName());
                } catch (Exception e) {
                    logger.error("Failed to unregister instance {}:{} to {} at {}, caused by {}",
                            instance.getHost(), instance.getPort(), name, cluster.getName(), e.getMessage(), e);
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
                cluster.subscribe(service, group, consumer);
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
                cluster.unsubscribe(service, group);
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

        SystemRegistryService(Callable<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void register(String service, String group, ServiceInstance instance) throws Exception {
            callback.call();
        }

        @Override
        protected List<ServiceEndpoint> getEndpoints(String service, String group) {
            return null;
        }

        @Override
        protected RegistryClusterConfig createDefaultConfig() {
            return new RegistryClusterConfig(RegistryRole.SYSTEM, RegistryMode.REGISTER);
        }
    }
}

