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

import java.util.function.Consumer;

/**
 * Provides functionality for managing the lifecycle of a registry service,
 * as well as registering, unregistering, subscribing, and unsubscribing from service instances.
 */
public interface RegistryService extends AutoCloseable {

    String getName();

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
    void subscribe(String service, String group, Consumer<EndpointEvent> consumer) throws Exception;

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
    class AbstractRegistryService implements RegistryService {

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
        public void subscribe(String service, String group, Consumer<EndpointEvent> consumer) throws Exception {

        }

        @Override
        public void unsubscribe(String service, String group) throws Exception {

        }
    }

}
