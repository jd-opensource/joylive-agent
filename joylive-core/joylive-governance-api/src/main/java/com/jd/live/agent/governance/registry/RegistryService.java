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

import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.config.RegistryMode;
import com.jd.live.agent.governance.config.RegistryRole;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Provides functionality for managing the lifecycle of a registry service,
 * as well as registering, unregistering, subscribing, and unsubscribing from service instances.
 */
public interface RegistryService extends AutoCloseable {

    String SYSTEM = "system";

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
    void register(String service, String group, ServiceInstance instance) throws Exception;

    /**
     * Unregisters a service instance from the registry.
     *
     * @param instance The service instance to be unregistered.
     */
    void unregister(String service, String group, ServiceInstance instance) throws Exception;

    /**
     * Subscribes to endpoint events for a specific service.
     *
     * @param service  The service name to subscribe to.
     * @param consumer The consumer that will receive endpoint events.
     */
    void subscribe(String service, String group, Consumer<RegistryEvent> consumer) throws Exception;

    /**
     * Unsubscribes from endpoint events for a specific service.
     *
     * @param service The service name to unsubscribe from.
     */
    void unsubscribe(String service, String group) throws Exception;

    /**
     * A basic implementation of the {@link RegistryService} interface.
     * This class provides foundational functionality for managing the lifecycle of a registry service,
     * as well as registering, unregistering, subscribing, and unsubscribing from service instances.
     */
    abstract class AbstractRegistryService implements RegistryService, RegistryEventPublisher {

        protected final List<RegistryListener> listeners = new CopyOnWriteArrayList<>();

        @Override
        public String getName() {
            return "unknown registry";
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
        public void register(String service, String group, ServiceInstance instance) throws Exception {

        }

        @Override
        public void unregister(String service, String group, ServiceInstance instance) throws Exception {

        }

        @Override
        public void subscribe(String service, String group, Consumer<RegistryEvent> consumer) throws Exception {
            RegistryListener listener = new RegistryListener(service, group, consumer);
            listeners.add(listener);
            try {
                List<ServiceEndpoint> endpoints = getEndpoints(service, group);
                publish(new RegistryEvent(service, group, endpoints, getDefaultGroup()), listener);
            } catch (Exception ignored) {
                // ignore
            }
        }

        @Override
        public void unsubscribe(String service, String group) throws Exception {
            listeners.removeIf(listener -> listener.match(service, group, getDefaultGroup()));
        }

        protected String getDefaultGroup() {
            return null;
        }

        protected abstract List<ServiceEndpoint> getEndpoints(String service, String group) throws Exception;

        @Override
        public void publish(RegistryEvent event) {
            if (event != null) {
                for (RegistryListener listener : listeners) {
                    publish(event, listener);
                }
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
            return listener.match(event.getService(), event.getGroup(), event.getDefaultGroup());
        }
    }

    /**
     * Base implementation for system-level registry services.
     *
     * @see AbstractRegistryService Parent service implementation
     */
    abstract class AbstractSystemRegistryService extends AbstractRegistryService {

        private final RegistryClusterConfig config;

        public AbstractSystemRegistryService() {
            config = createDefaultConfig();
        }

        @Override
        public String getName() {
            return SYSTEM;
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
