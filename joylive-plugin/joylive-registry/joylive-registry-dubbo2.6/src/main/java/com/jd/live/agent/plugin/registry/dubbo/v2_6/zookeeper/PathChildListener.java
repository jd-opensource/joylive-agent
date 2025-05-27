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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.zookeeper;

import lombok.Getter;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A container class that holds the configuration for a ZooKeeper child data listener.
 */
public class PathChildListener implements CuratorWatcher {

    @Getter
    private final String path;

    private final Set<ChildListener> listeners = new CopyOnWriteArraySet<>();

    private final PathChildWatcher watcher;

    private final Executor executor;

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Getter
    private List<String> children = new ArrayList<>();

    public PathChildListener(String path, PathChildWatcher watcher) {
        this(path, watcher, null);
    }

    public PathChildListener(String path, PathChildWatcher watcher, Executor executor) {
        this.path = path;
        this.watcher = watcher;
        this.executor = executor;
    }

    @Override
    public void process(WatchedEvent event) throws Exception {
        // if client connect or disconnect to server, zookeeper will queue
        // watched event(Watcher.Event.EventType.None, .., path = null).
        if (event.getType() == EventType.None) {
            return;
        }
        if (started.get()) {
            children = watcher.watch(path, this);
            for (ChildListener listener : listeners) {
                listener.childChanged(path, children);
            }
        }
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
     * Registers a listener for child node changes.
     *
     * @param listener The listener to add (non-null)
     */
    public boolean addListener(ChildListener listener) {
        return listeners.add(listener);
    }

    /**
     * Unregisters a child node listener.
     *
     * @param listener The listener to remove
     */
    public boolean removeListener(ChildListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Triggers all registered listeners with a child-changed event.
     * <p>Silently ignores any exceptions during notification.
     */
    public void update() {
        try {
            process(new WatchedEvent(EventType.NodeChildrenChanged, KeeperState.SyncConnected, path));
        } catch (Exception ignored) {
        }
    }

    /**
     * Checks if the listener is active.
     *
     * @return true if started, false otherwise
     */
    public boolean isStarted() {
        return started.get();
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            update();
        }
    }

    /**
     * Stops all listener notifications and cleans up resources.
     * <p>After closing, no more events will be delivered to listeners.
     */
    public void close() {
        started.compareAndSet(true, false);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PathChildListener)) return false;
        PathChildListener that = (PathChildListener) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }
}
