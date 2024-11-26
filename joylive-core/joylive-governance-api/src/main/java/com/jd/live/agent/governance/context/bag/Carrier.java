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
package com.jd.live.agent.governance.context.bag;

import com.jd.live.agent.bootstrap.util.Attributes;
import com.jd.live.agent.core.Constants;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Defines the contract for a carrier that transports data and attributes within a request.
 * <p>
 * This interface facilitates the management and transportation of request-specific data, known as "cargos", and attributes.
 * It provides methods for adding, retrieving, setting, and removing both cargos and attributes. Additionally, it allows for
 * iterating over cargos and attributes in various ways to suit different use cases.
 * </p>
 */
public interface Carrier extends Attributes {

    String ATTRIBUTE_FAILOVER_UNIT = Constants.LABEL_LIVE_PREFIX + "failover-unit";

    String ATTRIBUTE_FAILOVER_CELL = Constants.LABEL_LIVE_PREFIX + "failover-cell";

    String ATTRIBUTE_SERVICE_ID = Constants.LABEL_SERVICE_ID;

    String ATTRIBUTE_REQUEST = "request";

    String ATTRIBUTE_GATEWAY = Constants.LABEL_LIVE_PREFIX + "gateway";

    String ATTRIBUTE_DEADLINE = "deadline";

    String ATTRIBUTE_MQ_PRODUCER = "mq-producer";

    String ATTRIBUTE_RESTORE_BY = "restored-by";

    /**
     * Retrieves all cargos carried by this carrier.
     *
     * @return A collection of all cargos.
     */
    Collection<Cargo> getCargos();

    /**
     * Retrieves a specific cargo by key.
     *
     * @param key The key of the cargo to retrieve.
     * @return The cargo associated with the specified key, or null if not found.
     */
    Cargo getCargo(String key);

    /**
     * Adds a cargo to the carrier.
     *
     * @param cargo The cargo to add.
     */
    void addCargo(Cargo cargo);

    /**
     * Adds a cargo with the specified key and value to the carrier.
     *
     * @param key   The key of the cargo.
     * @param value The value of the cargo.
     */
    void addCargo(String key, String value);

    /**
     * Sets or replaces a cargo with the specified key and value.
     *
     * @param key   The key of the cargo.
     * @param value The value of the cargo.
     */
    void setCargo(String key, String value);

    /**
     * Removes a cargo by its key.
     *
     * @param key The key of the cargo to remove.
     */
    void removeCargo(String key);

    /**
     * Adds cargos based on a requirement and a map of potential cargos.
     *
     * @param require The requirement that must be met for a cargo to be added.
     * @param map     A map of potential cargos to add.
     */
    default void addCargo(CargoRequire require, Map<String, ? extends Collection<String>> map) {
        if (require != null && map != null) {
            for (Map.Entry<String, ? extends Collection<String>> entry : map.entrySet()) {
                if (require.match(entry.getKey())) {
                    addCargo(new Cargo(entry.getKey(), new ArrayList<>(entry.getValue())));
                }
            }
        }
    }

    /**
     * Adds cargos based on a requirement, a map of potential cargos, and a function to transform the map values.
     *
     * @param require The requirement that must be met for a cargo to be added.
     * @param map     A map of potential cargos to add.
     * @param func    A function to transform the map values.
     * @param <T>     The type of the map values.
     *  @param <M>    The type of the map extending from {@link Map} with keys as {@link String} and values of type {@code T}.
     */
    default <T, M extends Map<String, T>> void addCargo(CargoRequire require, M map, Function<String, Collection<String>> func) {
        if (require != null && map != null) {
            Object value;
            for (Map.Entry<String, T> entry : map.entrySet()) {
                if (require.match(entry.getKey())) {
                    value = entry.getValue();
                    if (value == null) {
                        addCargo(new Cargo(entry.getKey(), new ArrayList<>(0)));
                    } else {
                        addCargo(func == null ? new Cargo(entry.getKey(), value.toString()) :
                                new Cargo(entry.getKey(), func.apply(value.toString())));
                    }
                }
            }
        }
    }

    /**
     * Adds cargos based on a requirement, an enumeration of names, and a function to obtain values for those names.
     *
     * @param require The requirement that must be met for a cargo to be added.
     * @param names   An enumeration of names to consider for adding as cargos.
     * @param func    A function to obtain values for the names.
     */
    default void addCargo(CargoRequire require, Enumeration<String> names, Function<String, Enumeration<String>> func) {
        if (require != null && names != null) {
            String name;
            List<String> values;
            Enumeration<String> venum;
            while (names.hasMoreElements()) {
                name = names.nextElement();
                if (require.match(name)) {
                    values = null;
                    if (func != null) {
                        venum = func.apply(name);
                        values = venum == null ? null : Collections.list(venum);
                    }
                    addCargo(new Cargo(name, values));
                }
            }
        }
    }

    /**
     * Adds cargos based on a requirement, an iterable of names, and a function to obtain values for those names.
     *
     * @param require The requirement that must be met for a cargo to be added.
     * @param names   An iterable of names to consider for adding as cargos.
     * @param func    A function to obtain values for the names.
     */
    default void addCargo(CargoRequire require, Iterable<String> names, Function<String, List<String>> func) {
        if (require != null && names != null) {
            List<String> values;
            for (String name : names) {
                if (require.match(name)) {
                    values = null;
                    if (func != null) {
                        values = func.apply(name);
                    }
                    addCargo(new Cargo(name, values));
                }
            }
        }
    }

    /**
     * Adds cargos to the carrier based on a requirement, an iterable of target objects, and functions that provide keys and values.
     * <p>
     * This method allows adding multiple cargos at once by processing an iterable of target objects. For each target, the key and value
     * are determined by applying the provided functions. Only cargos with keys that match the requirement are added.
     * </p>
     *
     * @param <T>       The type of the target objects in the iterable.
     * @param require   The requirement that must be met for a cargo to be added.
     * @param targets   An iterable of target objects to process.
     * @param keyFunc   A function that generates a key for a cargo based on a target object.
     * @param valueFunc A function that generates a value for a cargo based on a target object (may be null).
     */
    default <T> void addCargo(CargoRequire require, Iterable<T> targets, Function<T, String> keyFunc, Function<T, String> valueFunc) {
        if (require != null && targets != null && keyFunc != null) {
            Map<String, List<String>> tags = new HashMap<>();
            for (T target : targets) {
                String key = keyFunc.apply(target);
                if (key != null) {
                    List<String> values = tags.computeIfAbsent(key, k -> new ArrayList<>());
                    String value = valueFunc == null ? null : valueFunc.apply(target);
                    if (value != null) {
                        values.add(value);
                    }
                }
            }
            for (Map.Entry<String, ? extends Collection<String>> entry : tags.entrySet()) {
                if (require.match(entry.getKey())) {
                    addCargo(new Cargo(entry.getKey(), entry.getValue()));
                }
            }
        }
    }

    /**
     * Traverses all cargos and processes each using the provided consumer.
     *
     * @param consumer A consumer to process each cargo.
     */
    default void cargos(Consumer<Cargo> consumer) {
        if (consumer != null) {
            Collection<Cargo> cargos = getCargos();
            if (cargos != null) {
                cargos.forEach(consumer);
            }
        }
    }

    /**
     * Traverses all cargos and processes each key-value pair using the provided bi-consumer.
     *
     * @param consumer A bi-consumer to process each key-value pair.
     */
    default void cargos(BiConsumer<String, String> consumer) {
        if (consumer != null) {
            Collection<Cargo> cargos = getCargos();
            if (cargos != null) {
                List<String> values;
                for (Cargo cargo : cargos) {
                    values = cargo.getValues();
                    int size = values == null ? 0 : values.size();
                    switch (size) {
                        case 0:
                            consumer.accept(cargo.getKey(), null);
                            break;
                        case 1:
                            consumer.accept(cargo.getKey(), values.get(0));
                            break;
                        default:
                            for (String value : values) {
                                consumer.accept(cargo.getKey(), value);
                            }
                    }
                }
            }
        }
    }

}
