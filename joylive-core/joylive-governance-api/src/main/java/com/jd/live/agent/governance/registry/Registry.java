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

import com.jd.live.agent.core.util.Futures;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * An interface that defines the methods for registering and unregistering service instances with a registry.
 */
public interface Registry extends ServiceRegistryFactory {

    /**
     * The name of the registry component.
     */
    String COMPONENT_REGISTRY = "Registry";

    /**
     * Associates an alias with the specified service name.
     *
     * @param alias the alternate name to assign
     * @param name  the canonical name of the service
     */
    void setServiceAlias(String alias, String name);

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
     * @param instances  the service instances to be registered
     * @param doRegister a doRegister function that will be called if the registration is successful
     */
    void register(List<ServiceInstance> instances, Callable<Void> doRegister);

    /**
     * Registers a service instance with the registry.
     *
     * @param instance   the service instance to be registered
     * @param doRegister a doRegister function that will be called if the registration is successful
     */
    default void register(ServiceInstance instance, Callable<Void> doRegister) {
        if (instance != null) {
            register(Collections.singletonList(instance), doRegister);
        }
    }

    /**
     * Unregisters a service instance from the registry.
     *
     * @param instance the service instance to be unregistered
     */
    void unregister(ServiceInstance instance);

    /**
     * Reregister a service instance with the registry.
     *
     * @param instance the service instance to be reregistered
     */
    default void reregister(ServiceInstance instance) {
        if (instance != null) {
            unregister(instance);
            register(instance);
        }
    }

    /**
     * Registers a service for policy subscription.
     *
     * @param service service name to register
     * @return future that completes when registration succeeds
     */
    default CompletableFuture<Void> register(String service) {
        return register(new ServiceId(service, null));
    }

    /**
     * Registers a service with group for policy subscription.
     *
     * @param service service name to register
     * @param group service group (may be null)
     * @return future that completes when registration succeeds
     */
    default CompletableFuture<Void> register(String service, String group) {
        return register(new ServiceId(service, group));
    }

    /**
     * Core registration method using ServiceId.
     *
     * @param serviceId complete service identifier
     * @return future that completes when registration succeeds
     */
    CompletableFuture<Void> register(ServiceId serviceId);

    /**
     * Subscribes to service policy notifications.
     *
     * @param service target service name
     * @return future completed when subscription succeeds
     */
    default CompletableFuture<Void> subscribe(String service) {
        return subscribe(new ServiceId(service));
    }

    /**
     * Subscribes to service policy notifications with group filtering.
     *
     * @param service target service name
     * @param group service group (optional)
     * @return future completed when subscription succeeds
     */
    default CompletableFuture<Void> subscribe(String service, String group) {
        return subscribe(new ServiceId(service, group));
    }

    /**
     * Core subscription method using complete service identifier.
     *
     * @param serviceId full service identification
     * @return future completed when subscription succeeds
     */
    CompletableFuture<Void> subscribe(ServiceId serviceId);

    /**
     * Subscribes to service endpoint events.
     *
     * @param service  target service name
     * @param consumer event handler for endpoint changes
     */
    default void subscribe(String service, Consumer<RegistryEvent> consumer) {
        subscribe(new ServiceId(service), consumer);
    }

    /**
     * Subscribes to service endpoint events with group filtering.
     *
     * @param service  target service name
     * @param group    service group (optional)
     * @param consumer event handler for endpoint changes
     */
    default void subscribe(String service, String group, Consumer<RegistryEvent> consumer) {
        subscribe(new ServiceId(service, group), consumer);
    }

    /**
     * Core subscription with full service identifier.
     *
     * @param serviceId complete service identification
     * @param consumer  event handler for endpoint changes
     */
    void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer);

    /**
     * Subscribes to the specified service and attempts to retrieve its governance policy.
     * If the subscription or policy retrieval fails, the provided error function is invoked to handle the error.
     *
     * @param <T>           the type of the result returned by the error function
     * @param service       the name of the service to subscribe to
     * @param errorFunction a {@link BiFunction} that handles errors by accepting an error message and a {@link Throwable}, and returns a result of type {@code T}
     * @return {@code null} if the subscription and policy retrieval are successful; otherwise, the result of the error function
     */
    default <T> T subscribe(String service, BiFunction<String, Throwable, T> errorFunction) {
        return subscribe(new ServiceId(service), 5000, TimeUnit.MILLISECONDS, errorFunction);
    }

    /**
     * Subscribes to the specified service and attempts to retrieve its governance policy.
     * If the subscription or policy retrieval fails, the provided error function is invoked to handle the error.
     *
     * @param <T>           the type of the result returned by the error function
     * @param serviceId     the id of the service to subscribe to
     * @param errorFunction a {@link BiFunction} that handles errors by accepting an error message and a {@link Throwable}, and returns a result of type {@code T}
     * @return {@code null} if the subscription and policy retrieval are successful; otherwise, the result of the error function
     */
    default <T> T subscribe(ServiceId serviceId, BiFunction<String, Throwable, T> errorFunction) {
        return subscribe(serviceId, 5000, TimeUnit.MILLISECONDS, errorFunction);
    }

    /**
     * Subscribes to the specified service and attempts to retrieve its governance policy.
     * If the subscription or policy retrieval fails, the provided error function is invoked to handle the error.
     *
     * @param <T>           the type of the result returned by the error function
     * @param service       the name of the service to subscribe to
     * @param timeout       the maximum time to wait for the subscription to complete
     * @param unit          the time unit of the timeout parameter
     * @param transformer   a {@link BiFunction} that handles errors by accepting an error message and a {@link Throwable}, and returns a result of type {@code T}
     * @return {@code null} if the subscription and policy retrieval are successful; otherwise, the result of the error function
     */
    default <T> T subscribe(String service, long timeout, TimeUnit unit, BiFunction<String, Throwable, T> transformer) {
        return subscribe(new ServiceId(service), timeout, unit, transformer);
    }

    /**
     * Subscribes to the specified service and group, and attempts to retrieve its governance policy.
     * If the subscription or policy retrieval fails, the provided error function is invoked to handle the error.
     *
     * @param <T>           the type of the result returned by the error function
     * @param service       the name of the service to subscribe to
     * @param group         the group associated with the service (optional, can be {@code null})
     * @param transformer   a {@link BiFunction} that handles errors by accepting an error message and a {@link Throwable}, and returns a result of type {@code T}
     * @return {@code null} if the subscription and policy retrieval are successful; otherwise, the result of the error function
     */
    default <T> T subscribe(String service, String group, BiFunction<String, Throwable, T> transformer) {
        return subscribe(new ServiceId(service, group), 5000, TimeUnit.MILLISECONDS, transformer);
    }

    /**
     * Subscribes to the specified service and group, and attempts to retrieve its governance policy.
     * If the subscription or policy retrieval fails, the provided error function is invoked to handle the error.
     *
     * @param <T>           the type of the result returned by the error function
     * @param service       the name of the service to subscribe to
     * @param group         the group associated with the service (optional, can be {@code null})
     * @param timeout       the maximum time to wait for the subscription to complete
     * @param unit          the time unit of the timeout parameter
     * @param transformer   a {@link BiFunction} that handles errors by accepting an error message and a {@link Throwable}, and returns a result of type {@code T}
     * @return {@code null} if the subscription and policy retrieval are successful; otherwise, the result of the error function
     */
    default <T> T subscribe(String service, String group, long timeout, TimeUnit unit, BiFunction<String, Throwable, T> transformer) {
        return subscribe(new ServiceId(service, group), timeout, unit, transformer);
    }

    /**
     * Subscribes to the specified service and group, and attempts to retrieve its governance policy.
     * If the subscription or policy retrieval fails, the provided error function is invoked to handle the error.
     *
     * @param <T>           the type of the result returned by the error function
     * @param serviceId     the id of the service to subscribe to
     * @param timeout       the maximum time to wait for the subscription to complete
     * @param unit          the time unit of the timeout parameter
     * @param transformer   a {@link BiFunction} that handles errors by accepting an error message and a {@link Throwable}, and returns a result of type {@code T}
     * @return {@code null} if the subscription and policy retrieval are successful; otherwise, the result of the error function
     */
    default <T> T subscribe(ServiceId serviceId, long timeout, TimeUnit unit, BiFunction<String, Throwable, T> transformer) {
        String action = "get governance policy for " + serviceId.getService();
        return Futures.invoke(subscribe(serviceId), timeout, unit, action, transformer);
    }

    /**
     * Removes endpoint event subscription for a service.
     *
     * @param service  service name to unsubscribe from
     * @param consumer event consumer to remove
     */
    default void unsubscribe(String service, Consumer<RegistryEvent> consumer) {
        unsubscribe(new ServiceId(service), consumer);
    }

    /**
     * Removes endpoint event subscription for a grouped service.
     *
     * @param service  service name to unsubscribe from
     * @param group    service group (may be null)
     * @param consumer event consumer to remove
     */
    default void unsubscribe(String service, String group, Consumer<RegistryEvent> consumer) {
        unsubscribe(new ServiceId(service, group), consumer);
    }

    /**
     * Core unsubscription with complete service identifier.
     *
     * @param serviceId full service identification
     * @param consumer  event consumer to remove
     */
    void unsubscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer);

    /**
     * Checks subscription status for a service (group-agnostic).
     *
     * @param service service name to check (non-null)
     * @return true if subscribed to the service
     * @see #isSubscribed(String, String)
     */
    default boolean isSubscribed(String service) {
        return isSubscribed(new ServiceId(service));
    }

    /**
     * Checks subscription status for a service and optional group.
     *
     * @param service service name to check (non-null)
     * @param group   consumer group (nullable)
     * @return true if subscribed to service/group combination
     */
    default boolean isSubscribed(String service, String group) {
        return isSubscribed(new ServiceId(service, group));
    }

    /**
     * Core subscription check using complete service identifier.
     *
     * @param serviceId full service identification
     * @return true if currently subscribed
     */
    boolean isSubscribed(ServiceId serviceId);

    /**
     * Verifies service readiness in default namespace.
     *
     * @param service service name to check (non-null and non-empty)
     * @return true if service is operational
     */
    default boolean isReady(String service) {
        return isReady(new ServiceId(service));
    }

    /**
     * Verifies service readiness in specified namespace.
     *
     * @param namespace target namespace (null for default)
     * @param service service name to check (non-null and non-empty)
     * @return true if service is operational in namespace
     */
    default boolean isReady(String namespace, String service) {
        return isReady(new ServiceId(namespace, service, null));
    }

    /**
     * Core readiness check with complete service identifier.
     *
     * @param serviceId full service identification
     * @return true if service is operational
     */
    boolean isReady(ServiceId serviceId);

    /**
     * Subscribes to a service and synchronously retrieves its endpoints with custom timeout.
     *
     * @param service     the service name to subscribe to
     * @param timeoutMs   the timeout in milliseconds for both subscription and retrieval
     * @param transformer function to handle and transform errors
     * @return list of available service endpoints
     * @throws Throwable if subscription fails or timeout occurs
     */
    default List<ServiceEndpoint> subscribeAndGet(final String service,
                                                  final long timeoutMs,
                                                  final BiFunction<String, Throwable, Throwable> transformer) throws Throwable {
        if (service == null || service.isEmpty()) {
            return null;
        }
        long time = System.currentTimeMillis();
        Throwable throwable = subscribe(service, timeoutMs, TimeUnit.MILLISECONDS, transformer);
        if (throwable != null) {
            throw throwable;
        }
        time = timeoutMs + time - System.currentTimeMillis();
        String action = "get service instances for " + service;
        return Futures.get(getEndpoints(service), time, TimeUnit.MILLISECONDS, action, transformer);
    }

    /**
     * Retrieves endpoints for the specified service using the default group.
     *
     * @param service the service name.
     * @return a list of endpoints associated with the service
     */
    default CompletionStage<List<ServiceEndpoint>> getEndpoints(final String service) {
        return getEndpoints(service, null, (ServiceRegistryFactory) null);
    }

    /**
     * Retrieves endpoints for the specified service and group.
     *
     * @param service the service name.
     * @param group   the service group.
     * @return a list of endpoints matching the service and group
     */
    default CompletionStage<List<ServiceEndpoint>> getEndpoints(final String service, final String group) {
        return getEndpoints(service, group, (ServiceRegistryFactory) null);
    }

    /**
     * Gets service endpoints for specified service and group combination.
     *
     * @param service the service name.
     * @param group   the service group.
     * @param system  the system provider
     * @return list of endpoints
     */
    default CompletionStage<List<ServiceEndpoint>> getEndpoints(final String service, final String group, final ServiceRegistry system) {
        return getEndpoints(service, group, name -> system);
    }

    /**
     * Gets service endpoints for specified service and group combination.
     *
     * @param service the service name.
     * @param group   the service group.
     * @param system  the system provider
     * @return list of endpoints
     */
    CompletionStage<List<ServiceEndpoint>> getEndpoints(String service, String group, ServiceRegistryFactory system);

    @Override
    default ServiceRegistry getServiceRegistry(String service) {
        return getServiceRegistry(new ServiceId(service));
    }

    /**
     * Gets the ServiceRegistry for a service in specified group.
     *
     * @param service target service name (non-null)
     * @param group   service group (null for default)
     * @return corresponding service registry
     */
    default ServiceRegistry getServiceRegistry(String service, String group) {
        return getServiceRegistry(new ServiceId(service, group));
    }

    /**
     * Gets the ServiceRegistry with complete service identification.
     *
     * @param serviceId full service identifier
     * @return corresponding service registry
     */
    ServiceRegistry getServiceRegistry(ServiceId serviceId);

}

