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

import com.jd.live.agent.governance.instance.Endpoint;

import java.util.List;
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
     * @param instance   the service instance to be registered
     * @param doRegister a doRegister function that will be called if the registration is successful
     */
    void register(ServiceInstance instance, Callable<Void> doRegister);

    /**
     * Unregisters a service instance from the registry.
     *
     * @param instance the service instance to be unregistered
     */
    void unregister(ServiceInstance instance);

    /**
     * Subscribes to a specific service policy based on the service name.
     *
     * @param service The name of the service to register.
     * @return A {@link CompletableFuture} that completes when the registration is successful.
     */
    default CompletableFuture<Void> register(String service) {
        return register(service, null);
    }

    /**
     * Subscribes to a specific service policy based on the service name and group.
     *
     * @param service The name of the service to register.
     * @param group   The group to which the service belongs.
     * @return A {@link CompletableFuture} that completes when the registration is successful.
     */
    CompletableFuture<Void> register(String service, String group);

    /**
     * Subscribes to a specific service policy based on the service name .
     *
     * @param service The name of the service whose policy is to be subscribed to.
     * @return A {@link CompletableFuture} that completes when the subscription is successful.
     */
    default CompletableFuture<Void> subscribe(String service) {
        return subscribe(service, (String) null);
    }

    /**
     * Subscribes to a specific service policy based on the service name and group.
     *
     * @param service The name of the service whose policy is to be subscribed to.
     * @param group   The group to which the service belongs.
     * @return A {@link CompletableFuture} that completes when the subscription is successful.
     */
    CompletableFuture<Void> subscribe(String service, String group);

    /**
     * Subscribes to endpoint events for a specific service.
     *
     * @param service  the service name to subscribe to
     * @param consumer the consumer that will receive endpoint events
     */
    default void subscribe(String service, Consumer<EndpointEvent> consumer) {
        subscribe(service, null, consumer);
    }

    /**
     * Subscribes to a specific service in the specified group and registers a consumer to handle endpoint events.
     * This method allows the caller to receive notifications or updates related to the service's endpoints.
     *
     * @param service  the name of the service to subscribe to.
     * @param group    the group to which the service belongs.
     * @param consumer the consumer to handle endpoint events triggered by the subscription.
     */
    void subscribe(String service, String group, Consumer<EndpointEvent> consumer);

    /**
     * Retrieves endpoints for the specified service using the default group.
     *
     * @param service the name of the target service
     * @return a list of endpoints associated with the service (never null)
     */
    default List<? extends Endpoint> getEndpoints(String service) {
        return getEndpoints(service, null);
    }

    /**
     * Retrieves endpoints for the specified service and group.
     *
     * @param service the name of the target service
     * @param group   the cluster/group name (may be null for default group)
     * @return a list of endpoints matching the service and group (never null)
     */
    List<? extends Endpoint> getEndpoints(String service, String group);

}

