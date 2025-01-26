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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An interface that defines the methods for registering and unregistering service instances with a registry.
 */
public interface Registry {

    /**
     * The name of the registry component.
     */
    String COMPONENT_REGISTRY = "Registry";

    /**
     * Registers a service instance with the registry.
     *
     * @param instance the service instance to be registered
     */
    default void register(ServiceInstance instance) {
        register(instance, null);
    }

    /**
     * Registers a service instance with the registry.
     *
     * @param instance the service instance to be registered
     * @param callback a callback function that will be called if the registration is successful
     */
    void register(ServiceInstance instance, Callable<Void> callback);

    /**
     * Unregisters a service instance from the registry.
     *
     * @param instance the service instance to be unregistered
     */
    void unregister(ServiceInstance instance);

    /**
     * Subscribes a specific service policy based on its name.
     *
     * @param service The service name of the policy to subscribe to.
     * @return A {@link CompletableFuture} that completes when the subscription is successful.
     */
    CompletableFuture<Void> subscribe(String service);

    /**
     * Subscribes to endpoint events for a specific service.
     *
     * @param service  the service name to subscribe to
     * @param consumer the consumer that will receive endpoint events
     */
    void subscribe(String service, Consumer<EndpointEvent> consumer);
}

