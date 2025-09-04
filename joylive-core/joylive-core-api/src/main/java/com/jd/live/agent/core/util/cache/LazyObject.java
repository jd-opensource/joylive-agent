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
 * <p>This class ensures that the object is loaded only once, even in a multi-threaded environment, and that
 * subsequent calls to {@link #get()} return the same instance without re-evaluating the supplier.</p>
 *
 * @param <T> The type of the object to be lazily initialized.
 */
public class LazyObject<T> extends CacheObject<T> {

    /**
     * The supplier used to create the object on demand.
     */
    private Supplier<T> supplier;

    /**
     * A flag indicating whether the object has been loaded.
     */
    private volatile boolean loaded;

    protected LazyObject(final T target, final boolean loaded, final Supplier<T> supplier) {
        super(target);
        this.loaded = loaded;
        this.supplier = supplier;
    }

    /**
     * Constructor that initializes the LazyObject with a specific target object, marking it as loaded.
     *
     * @param target The object to be cached.
     */
    public LazyObject(final T target) {
        this(target, true, null);
    }

    /**
     * Constructor that sets the Supplier for lazy initialization of the object.
     *
     * @param supplier The Supplier to use for creating the object when needed.
     */
    public LazyObject(final Supplier<T> supplier) {
        this(null, false, supplier);
    }

    /**
     * Retrieves the cached object, loading it from the Supplier if it has not been loaded yet. The object is
     * loaded in a thread-safe manner, ensuring that the Supplier is only called once.
     *
     * @return The cached object of type T.
     */
    public T get() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    target = supplier == null ? null : supplier.get();
                    loaded = true;
                }
            }
        }
        return target;
    }

    /**
     * Gets the value using lazy initialization with double-checked locking.
     *
     * @param supplier the supplier to provide the value if not already loaded
     * @return the cached value or newly supplied value
     */
    public T get(final Supplier<T> supplier) {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    target = supplier == null ? null : supplier.get();
                    loaded = true;
                }
            }
        }
        return target;
    }

    /**
     * Static factory method that creates a new LazyObject with the specified target object, marking it as
     * loaded.
     *
     * @param <T> The type of the object to be cached.
     * @param target The object to be cached.
     * @return A new LazyObject containing the specified target object.
     */
    public static <T> LazyObject<T> of(T target) {
        return new LazyObject<>(target);
    }

    /**
     * Creates an "empty" {@code LazyObject} instance.
     *
     * @param <T> The type of the object that this {@code LazyObject} is meant to encapsulate.
     * @return An "empty" {@code LazyObject} instance, which will return {@code null} for any attempt to get its value.
     */
    public static <T> LazyObject<T> empty() {
        return new LazyObject<>(null);
    }


    /**
     * Static factory method that creates a new LazyObject with the specified Supplier for lazy
     * initialization.
     *
     * @param <T> The type of the object to be cached.
     * @param supplier The Supplier to use for creating the object when needed.
     * @return A new LazyObject using the specified Supplier for lazy initialization.
     */
    public static <T> LazyObject<T> of(Supplier<T> supplier) {
        return new LazyObject<>(supplier);
    }

}

