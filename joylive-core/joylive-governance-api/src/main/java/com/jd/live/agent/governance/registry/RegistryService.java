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

import com.jd.live.agent.core.util.Locks;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.config.RegistryMode;
import com.jd.live.agent.governance.config.RegistryRole;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Provides functionality for managing the lifecycle of a registry service,
 * as well as registering, unregistering, subscribing, and unsubscribing from service instances.
 */
public interface RegistryService extends AutoCloseable {

    String SYSTEM = "system";

    String SYSTEM_REGISTERED = "true";

    String KEY_SYSTEM_REGISTERED = "system_registered";

    Predicate<String> SYSTEM_REGISTERED_PREDICATE = SYSTEM_REGISTERED::equalsIgnoreCase;

    /**
     * Retrieves the name of the registry service.
     *
     * @return The name of the registry service as a string. This value should uniquely identify
     * the registry service instance.
     */
    String getName();

    /**
     * Retrieves the description of the registry service.
     *
     * @return The description of the registry service.
     */
    default String getDescription() {
        return getName();
    }

    /**
     * Retrieves the configuration of the registry cluster associated with this service.
     *
     * @return The {@link RegistryClusterConfig} object representing the configuration of the
     *         registry cluster. This includes settings such as cluster nodes, connection details,
     *         and other relevant parameters.
     */
    RegistryClusterConfig getConfig();

    /**
     * Starts the registry service, initializing any necessary resources or connections.
     */
    void start() throws Exception;

    @Override
    void close();

    /**
     * Registers a service instance with the registry.
     *
     * @param instance The service instance to be registered.
     */
    default void register(ServiceInstance instance) throws Exception {
        register(instance, instance);
    }

    /**
     * Registers a service instance under specified service and group.
     *
     * @param service  service name to register under (non-null)
     * @param group    service group to register under (nullable for default group)
     * @param instance service instance to register (non-null)
     * @throws Exception if registration fails due to validation or system errors
     * @see #register(ServiceId, ServiceInstance)
     */
    default void register(String service, String group, ServiceInstance instance) throws Exception {
        register(new ServiceId(service, group), instance);
    }

    /**
     * Core method to register a service instance with complete identification.
     *
     * @param serviceId full service identifier (non-null)
     * @param instance  service instance to register (non-null)
     * @throws Exception if registration fails due to validation or system errors
     */
    void register(ServiceId serviceId, ServiceInstance instance) throws Exception;

    /**
     * Unregisters a service instance from the registry.
     *
     * @param instance The service instance to be unregistered.
     */
    default void unregister(ServiceInstance instance) throws Exception {
        unregister(instance, instance);
    }

    /**
     * Unregisters a service instance from the specified service group.
     *
     * @param service  the service name the instance belongs to (must not be null)
     * @param group    the service group (null indicates default group)
     * @param instance the service instance to unregister (must not be null)
     * @throws Exception if unregistration fails due to system errors or invalid parameters
     * @see #unregister(ServiceId, ServiceInstance)
     */
    default void unregister(String service, String group, ServiceInstance instance) throws Exception {
        unregister(new ServiceId(service, group), instance);
    }

    /**
     * Core method to unregister a service instance using complete service identification.
     *
     * @param serviceId the full service identifier (must not be null)
     * @param instance  the service instance to unregister (must not be null)
     * @throws Exception if unregistration fails due to system errors or invalid parameters
     */
    void unregister(ServiceId serviceId, ServiceInstance instance) throws Exception;

    /**
     * Subscribes to service registry events for a specific service group.
     *
     * @param service  service name to subscribe to (must not be null)
     * @param group    service group to monitor (null indicates default group)
     * @param consumer event consumer that will receive registry events (must not be null)
     * @throws Exception if subscription fails due to system errors or invalid parameters
     * @see #subscribe(ServiceId, Consumer)
     */
    default void subscribe(String service, String group, Consumer<RegistryEvent> consumer) throws Exception {
        subscribe(new ServiceId(service, group), consumer);
    }

    /**
     * Core method to subscribe to service registry events with complete identification.
     *
     * @param serviceId full service identifier (must not be null)
     * @param consumer  event consumer that will receive registry events (must not be null)
     * @throws Exception if subscription fails due to system errors or invalid parameters
     */
    void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) throws Exception;

    /**
     * Unsubscribes from service registry events for a specific service group.
     *
     * @param service service name to unsubscribe from (must not be null)
     * @param group   service group to stop monitoring (null indicates default group)
     * @throws Exception if unsubscription fails due to system errors or invalid parameters
     * @see #unsubscribe(ServiceId)
     */
    default void unsubscribe(String service, String group) throws Exception {
        unsubscribe(new ServiceId(service, group));
    }

    /**
     * Core method to unsubscribe from service registry events with complete identification.
     *
     * @param serviceId full service identifier (must not be null)
     * @throws Exception if unsubscription fails due to system errors or invalid parameters
     */
    void unsubscribe(ServiceId serviceId) throws Exception;

    /**
     * A basic implementation of the {@link RegistryService} interface.
     * This class provides foundational functionality for managing the lifecycle of a registry service,
     * as well as registering, unregistering, subscribing, and unsubscribing from service instances.
     */
    abstract class AbstractRegistryService implements RegistryService, RegistryEventPublisher {

        protected final List<RegistryListener> listeners = new CopyOnWriteArrayList<>();

        protected final Map<String, RegistryEvent> events = new ConcurrentHashMap<>();

        protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        protected final String name;

        public AbstractRegistryService(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public RegistryClusterConfig getConfig() {
            return null;
        }

        @Override
        public void start() throws Exception {

        }

        @Override
        public void close() {

        }

        @Override
        public void register(ServiceId serviceId, ServiceInstance instance) throws Exception {

        }

        @Override
        public void unregister(ServiceId serviceId, ServiceInstance instance) throws Exception {

        }

        @Override
        public void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) throws Exception {
            Locks.write(lock, () -> {
                RegistryListener listener = new RegistryListener(serviceId, consumer);
                listeners.add(listener);
                // notify
                String key = serviceId.getUniqueName();
                RegistryEvent event = events.get(key);
                if (event == null) {
                    try {
                        event = new RegistryEvent(serviceId, getEndpoints(serviceId), getDefaultGroup());
                        RegistryEvent old = events.putIfAbsent(key, event);
                        if (old != null) {
                            event = old;
                        }
                    } catch (Exception ignored) {
                        // ignore
                    }
                }
                publish(event, listener);
            });
        }

        @Override
        public void unsubscribe(ServiceId serviceId) throws Exception {
            listeners.removeIf(listener -> listener.match(serviceId, getDefaultGroup()));
        }

        protected String getDefaultGroup() {
            return null;
        }

        protected abstract List<ServiceEndpoint> getEndpoints(ServiceId serviceId) throws Exception;

        @Override
        public void publish(RegistryEvent event) {
            if (event != null) {
                if (event.isFull()) {
                    String key = event.getServiceId().getUniqueName();
                    events.put(key, event);
                }
                Locks.read(lock, () -> {
                    for (RegistryListener listener : listeners) {
                        publish(event, listener);
                    }
                });
            }
        }

        /**
         * Delivers an event to a specific listener if service/group conditions match.
         */
        protected void publish(RegistryEvent event, RegistryListener listener) {
            if (event != null && listener != null && match(event, listener)) {
                listener.publish(event);
            }
        }

        protected boolean match(RegistryEvent event, RegistryListener listener) {
            return listener.match(event.getServiceId(), event.getDefaultGroup());
        }
    }

    /**
     * Base implementation for system-level registry services.
     *
     * @see AbstractRegistryService Parent service implementation
     */
    abstract class AbstractSystemRegistryService extends AbstractRegistryService {

        private final RegistryClusterConfig config;

        public AbstractSystemRegistryService(String name) {
            super(name == null ? SYSTEM : name);
            this.config = createDefaultConfig();
        }

        @Override
        public RegistryClusterConfig getConfig() {
            return config;
        }

        protected RegistryClusterConfig createDefaultConfig() {
            // only subscribe mode is supported for system
            return new RegistryClusterConfig(RegistryRole.SYSTEM, RegistryMode.SUBSCRIBE);
        }
    }

}
