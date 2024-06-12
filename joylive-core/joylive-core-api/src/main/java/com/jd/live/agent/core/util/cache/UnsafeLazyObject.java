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
package com.jd.live.agent.core.util.cache;

import java.util.function.Supplier;

/**
 * A generic class that extends {@link CacheObject} to provide lazy initialization of the cached object. The
 * object is only created and cached when the {@link #get()} method is first called. This class uses a
 * {@link Supplier} to create the object on demand, which can be useful for expensive operations that should
 * only be performed if necessary.
 *
 * @param <T> The type of the object to be lazily initialized.
 */
public class UnsafeLazyObject<T> extends CacheObject<T> {

    /**
     * The supplier used to create the object on demand.
     */
    private final Supplier<T> supplier;

    /**
     * A flag indicating whether the object has been loaded.
     */
    private boolean loaded;

    /**
     * Constructor that sets the Supplier for lazy initialization of the object.
     *
     * @param supplier The Supplier to use for creating the object when needed.
     */
    public UnsafeLazyObject(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Retrieves the cached object, loading it from the Supplier if it has not been loaded yet. The object is
     * loaded in a thread-safe manner, ensuring that the Supplier is only called once.
     *
     * @return The cached object of type T.
     */
    @Override
    public T get() {
        if (!loaded && supplier != null) {
            target = supplier.get();
            loaded = true;
        }
        return target;
    }

    /**
     * Creates an "empty" {@code LazyObject} instance.
     *
     * @param <T> The type of the object that this {@code LazyObject} is meant to encapsulate.
     * @return An "empty" {@code LazyObject} instance, which will return {@code null} for any attempt to get its value.
     */
    public static <T> UnsafeLazyObject<T> empty() {
        return new UnsafeLazyObject<>(null);
    }

    /**
     * Static factory method that creates a new LazyObject with the specified Supplier for lazy
     * initialization.
     *
     * @param <T>      The type of the object to be cached.
     * @param supplier The Supplier to use for creating the object when needed.
     * @return A new LazyObject using the specified Supplier for lazy initialization.
     */
    public static <T> UnsafeLazyObject<T> of(Supplier<T> supplier) {
        return new UnsafeLazyObject<>(supplier);
    }

}

