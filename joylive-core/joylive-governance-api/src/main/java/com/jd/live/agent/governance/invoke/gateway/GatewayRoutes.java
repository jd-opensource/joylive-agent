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
package com.jd.live.agent.governance.invoke.gateway;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * An abstract class representing the version management for live routes.
 * This class provides a global counter to track and manage the version of routes,
 * allowing for version retrieval and increment operations.
 */
public abstract class GatewayRoutes {

    /**
     * A global atomic counter that tracks the current version of the routes.
     * This counter is used to ensure consistency and track changes in route configurations.
     */
    private static final AtomicLong ROUTE_VERSION = new AtomicLong(0);

    /**
     * A thread-safe map that stores live route instances, keyed by their unique identifiers.
     */
    private static final Map<String, GatewayRoute<?>> ROUTES = new ConcurrentHashMap<>();

    /**
     * Retrieves the current value of the global route version counter.
     *
     * @return the current route version as a long value
     */
    public static long getVersion() {
        return ROUTE_VERSION.get();
    }

    /**
     * Increments the global route version counter by one.
     * This method is used to signal a change in the route configuration.
     */
    public static void incVersion() {
        ROUTE_VERSION.incrementAndGet();
    }

    /**
     * Retrieves an existing live route from the map by its identifier, or creates a new one
     * using the provided function if it does not already exist.
     *
     * @param id       the unique identifier of the live route
     * @param function a function to create a new live route if it does not exist
     * @return the existing or newly created live route
     */
    private static GatewayRoute<?> getOrCreate(String id, Function<String, GatewayRoute<?>> function) {
        return ROUTES.computeIfAbsent(id, function);
    }

    /**
     * Retrieves a live route from the map by its identifier.
     *
     * @param id the unique identifier of the live route
     * @return the live route associated with the identifier, or {@code null} if not found
     */
    @SuppressWarnings("unchecked")
    public static <R> GatewayRoute<R> get(String id) {
        return (GatewayRoute<R>) ROUTES.get(id);
    }

    /**
     * Adds or updates a live route in the map with the specified identifier.
     *
     * @param id    the unique identifier of the live route
     * @param route the live route to store
     */
    private static void put(String id, GatewayRoute<?> route) {
        ROUTES.put(id, route);
    }

    /**
     * Updates the gateway route cache with the given route if conditions are met.
     *
     * @param <R>      the route type
     * @param route    the route to update
     * @param id       the route identifier
     * @param supplier the route definition supplier
     */
    public static <R> void update(R route, String id, Object supplier) {
        if (supplier instanceof GatewayRouteDefSupplier) {
            GatewayRouteDef routeDef = ((GatewayRouteDefSupplier) supplier).getDefinition();
            Object definition = routeDef.getDefinition();
            Function<String, GatewayRoute<?>> routeFunction = i -> new GatewayRoute<>(route, definition, routeDef.getVersion());
            GatewayRoute<?> oldRoute = getOrCreate(id, routeFunction);
            if (oldRoute.getRoute() != route
                    && routeDef.getVersion() != oldRoute.getVersion()
                    && (definition == null || !Objects.equals(definition, oldRoute.getDefinition()))) {
                put(id, routeFunction.apply(id));
            }
        }

    }
}
