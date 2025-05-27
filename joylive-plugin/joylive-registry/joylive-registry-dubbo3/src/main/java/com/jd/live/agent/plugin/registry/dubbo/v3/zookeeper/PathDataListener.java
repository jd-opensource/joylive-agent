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
package com.jd.live.agent.plugin.registry.dubbo.v3.zookeeper;

import lombok.Getter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.EventType;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A container class that holds the configuration for a ZooKeeper path data listener.
 */
@SuppressWarnings("deprecation")
public class PathDataListener implements TreeCacheListener {

    @Getter
    private final String path;

    private final Set<DataListener> listeners = new CopyOnWriteArraySet<>();

    private final Executor executor;

    private final Function<String, TreeCache> cacheFactory;

    private volatile TreeCache cache;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public PathDataListener(String path, Function<String, TreeCache> cacheFactory) {
        this(path, cacheFactory, null);
    }

    public PathDataListener(String path, Function<String, TreeCache> cacheFactory, Executor executor) {
        this.path = path;
        this.executor = executor;
        this.cacheFactory = cacheFactory;
    }

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        if (!started.get()) {
            return;
        }

        TreeCacheEvent.Type type = event.getType();
        ChildData data = event.getData();
        switch (type) {
            case NODE_ADDED:
                notify(data, EventType.NodeCreated);
                break;
            case NODE_UPDATED:
                notify(data, EventType.NodeDataChanged);
                break;
            case NODE_REMOVED:
                notify(data.getPath(), null, EventType.NodeDeleted);
                break;
            case CONNECTION_LOST:
                notify(null, null, EventType.CONNECTION_LOST);
                break;
            case CONNECTION_RECONNECTED:
                notify(null, null, EventType.CONNECTION_RECONNECTED);
                break;
            case CONNECTION_SUSPENDED:
                notify(null, null, EventType.CONNECTION_SUSPENDED);
                break;
            case INITIALIZED:
            default:
                notify(null, null, EventType.INITIALIZED);
                break;
        }
    }

    /**
     * Adds a data listener.
     *
     * @param listener The listener to add
     * @return true if listener was added, false if already present
     */
    public boolean addListener(DataListener listener) {
        return listeners.add(listener);
    }

    /**
     * Removes a data listener.
     *
     * @param listener The listener to remove
     * @return true if listener was removed, false if not found
     */
    public boolean removeListener(DataListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Checks if no listeners are registered.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    /**
     * Checks if the cache is active.
     *
     * @return true if started, false otherwise
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * Starts the cache and initializes listeners.
     *
     * @throws IllegalStateException if startup fails
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            try {
                update();
            } catch (RuntimeException e) {
                started.set(false);
                throw e;
            } catch (Throwable e) {
                started.set(false);
                throw new IllegalStateException("Failed to start the zookeeper cache", e);
            }
        }
    }

    /**
     * Recreates the cache instance.
     *
     * @throws IllegalStateException if recreation fails
     */
    public void update() {
        TreeCache cache = cacheFactory.apply(path);
        cache.getListenable().addListener(this);
        try {
            cache.start();
            this.cache = cache;
        } catch (Throwable e) {
            cache.close();
            cache.getListenable().removeListener(this);
            throw new IllegalStateException("Failed to recreate the zookeeper cache", e);
        }
    }

    /**
     * Stops the cache and cleans up resources.
     * <p>
     * Atomic operation - only succeeds if currently active.
     */
    public void close() {
        if (started.compareAndSet(true, false)) {
            cache.close();
            cache.getListenable().removeListener(this);
        }
    }

    /**
     * Notifies all listeners of a data change.
     *
     * @param path Changed node path
     * @param data New node data
     * @param type Event type
     */
    private void notify(String path, Object data, EventType type) {
        for (DataListener listener : listeners) {
            listener.dataChanged(path, data, type);
        }
    }

    /**
     * Notifies all listeners of child data change.
     *
     * @param data Changed child data
     * @param type Event type
     */
    private void notify(ChildData data, EventType type) {
        String path = data.getPath();
        String value = data.getData() == null ? "" : new String(data.getData(), StandardCharsets.UTF_8);
        notify(path, value, type);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PathDataListener)) return false;
        PathDataListener that = (PathDataListener) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }
}
