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
package com.jd.live.agent.governance.context;

import com.jd.live.agent.governance.context.bag.Cargo;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Courier;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Provides a context for managing request-specific data within the lifetime of a single request.
 * <p>
 * Utilizes a {@link ThreadLocal} to store and manage {@link Carrier} instances, ensuring data is isolated to individual threads.
 * This class supports creating, retrieving, and managing the lifecycle of {@link Carrier} instances to facilitate the passing
 * of request-specific data across different layers of an application.
 * </p>
 *
 * @since 1.0.0
 */
public class RequestContext {

    /**
     * Thread-local storage for {@link Carrier} instances, allowing data to be inherited by child threads.
     */
    private static final ThreadLocal<Carrier> CARRIER = new InheritableThreadLocal<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private RequestContext() {
    }

    /**
     * Retrieves the current {@link Carrier} instance associated with the current thread, if any.
     *
     * @return The current {@link Carrier} instance, or {@code null} if none is set.
     */
    public static Carrier get() {
        return CARRIER.get();
    }

    /**
     * Retrieves the current {@link Carrier} instance, or creates and sets a new instance if none exists.
     *
     * @return The current {@link Carrier} instance, or a new instance if none was previously set.
     */
    public static Carrier getOrCreate() {
        Carrier carrier = CARRIER.get();
        if (carrier == null) {
            carrier = new Courier();
            CARRIER.set(carrier);
        }
        return carrier;
    }

    /**
     * Creates and sets a new {@link Carrier} instance for the current thread.
     *
     * @return The newly created {@link Carrier} instance.
     */
    public static Carrier create() {
        Carrier carrier = new Courier();
        CARRIER.set(carrier);
        return carrier;
    }

    /**
     * Sets the specified {@link Carrier} instance for the current thread.
     * <p>
     * If the provided {@link Carrier} is {@code null}, the current carrier is removed.
     * </p>
     *
     * @param carrier The {@link Carrier} instance to set, or {@code null} to remove the current instance.
     */
    public static void set(Carrier carrier) {
        if (carrier == null) {
            CARRIER.remove();
        } else {
            CARRIER.set(carrier);
        }
    }

    /**
     * Removes the current {@link Carrier} instance from the current thread.
     */
    public static void remove() {
        CARRIER.remove();
    }

    /**
     * Traverses the contents of the current {@link Carrier} instance using the provided {@link Consumer}.
     *
     * @param consumer The {@link Consumer} to process each {@link Cargo} contained in the {@link Carrier}.
     */
    public static void traverse(Consumer<Cargo> consumer) {
        Carrier carrier = CARRIER.get();
        if (carrier != null) {
            carrier.traverse(consumer);
        }
    }

    /**
     * Traverses the contents of the current {@link Carrier} instance using the provided {@link BiConsumer}.
     *
     * @param consumer The {@link BiConsumer} to process each key-value pair contained in the {@link Carrier}.
     */
    public static void traverse(BiConsumer<String, String> consumer) {
        Carrier carrier = CARRIER.get();
        if (carrier != null) {
            carrier.traverse(consumer);
        }
    }

    /**
     * Retrieves a specific {@link Cargo} by key from the current {@link Carrier} instance.
     *
     * @param key The key associated with the {@link Cargo} to retrieve.
     * @return The {@link Cargo} associated with the specified key, or {@code null} if not found.
     */
    public static Cargo getCargo(String key) {
        Carrier carrier = CARRIER.get();
        return carrier == null ? null : carrier.getCargo(key);
    }

    /**
     * Retrieves a specific attribute by key from the current {@link Carrier} instance.
     *
     * @param <T> The type of the attribute to retrieve.
     * @param key The key associated with the attribute to retrieve.
     * @return The attribute associated with the specified key, or {@code null} if not found.
     */
    public static <T> T getAttribute(String key) {
        Carrier carrier = CARRIER.get();
        return carrier == null ? null : carrier.getAttribute(key);
    }

    /**
     * Removes a specific attribute by key from the current {@link Carrier} instance.
     *
     * @param <T> The type of the attribute to remove.
     * @param key The key associated with the attribute to remove.
     * @return The removed attribute, or {@code null} if the attribute was not found.
     */
    public static <T> T removeAttribute(String key) {
        Carrier carrier = CARRIER.get();
        return carrier == null ? null : carrier.removeAttribute(key);
    }

    /**
     * Checks if the current {@link Carrier} instance contains any {@link Cargo}.
     *
     * @return {@code true} if the current {@link Carrier} contains {@link Cargo}, {@code false} otherwise.
     */
    public static boolean hasCargo() {
        Carrier carrier = CARRIER.get();
        return carrier != null && carrier.getCargos() != null && !carrier.getCargos().isEmpty();
    }

    /**
     * Determines if the current time has exceeded a specified deadline.
     */
    public static boolean isTimeout() {
        Long deadline = getAttribute(Carrier.ATTRIBUTE_DEADLINE);
        return deadline != null && System.currentTimeMillis() > deadline;
    }
}

