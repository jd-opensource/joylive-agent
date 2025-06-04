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
package com.jd.live.agent.implement.service.registry.zookeeper.dubbo;

import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.ServiceId;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A wrapper implementation of {@link CuratorCache} that adds atomic start/close control
 * and delegates all operations to the underlying CuratorCache instance.
 */
public class DubboCuratorCache implements CuratorCache {

    private final ServiceId serviceId;

    private final String path;

    private final Consumer<RegistryEvent> consumer;

    private final DubboCuratorCacheFactory factory;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile CuratorCache cache;

    public DubboCuratorCache(ServiceId serviceId,
                             String path,
                             Consumer<RegistryEvent> consumer,
                             DubboCuratorCacheFactory factory) {
        this.serviceId = serviceId;
        this.path = path;
        this.consumer = consumer;
        this.factory = factory;
        this.cache = factory.create(serviceId, path, consumer);
    }

    public boolean isStarted() {
        return started.get();
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            try {
                cache.start();
            } catch (RuntimeException e) {
                started.set(false);
                throw e;
            } catch (Exception e) {
                started.set(false);
                throw new IllegalStateException("Failed to create curator cache.", e);
            }
        }
    }

    public void recreate() {
        if (!isStarted()) {
            start();
        } else {
            cache.close();
            cache = factory.create(serviceId, path, consumer);
            cache.start();
        }
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            cache.close();
        }
    }

    @Override
    public Listenable<CuratorCacheListener> listenable() {
        return cache.listenable();
    }

    @Override
    public Optional<ChildData> get(String path) {
        return cache.get(path);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public Stream<ChildData> stream() {
        return cache.stream();
    }
}
