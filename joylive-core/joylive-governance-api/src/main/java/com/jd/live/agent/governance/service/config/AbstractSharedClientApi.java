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
package com.jd.live.agent.governance.service.config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * An abstract implementation of a shared client API that manages connection state and reference counting.
 * It ensures clients are reused and cleaned up when no longer referenced.
 *
 * @param <T> The type of the underlying ConfigClientApi implementation.
 */
public abstract class AbstractSharedClientApi<T extends ConfigClientApi> implements ConfigClientApi {

    protected final String name;

    protected final T api;

    protected final Consumer<String> cleaner;

    protected final AtomicInteger reference = new AtomicInteger(0);

    protected final AtomicBoolean connected = new AtomicBoolean(false);

    public AbstractSharedClientApi(String name, T api, Consumer<String> cleaner) {
        this.name = name;
        this.api = api;
        this.cleaner = cleaner;
    }

    @Override
    public void connect() throws Exception {
        if (!connected.get()) {
            synchronized (api) {
                if (connected.compareAndSet(false, true)) {
                    api.connect();
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (reference.decrementAndGet() == 0) {
            cleaner.accept(name);
            if (connected.get()) {
                synchronized (api) {
                    if (connected.compareAndSet(true, false)) {
                        api.close();
                    }
                }
            }
        }
    }

    /**
     * Increments the reference count for this shared client.
     */
    public void incReference() {
        reference.incrementAndGet();
    }
}
