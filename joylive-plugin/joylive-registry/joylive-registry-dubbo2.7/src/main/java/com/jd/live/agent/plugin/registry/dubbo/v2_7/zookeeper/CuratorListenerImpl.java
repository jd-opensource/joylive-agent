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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.EventType;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link CuratorListener} that delegates events to either:
 * <ul>
 *   <li>A {@link ChildListener} for path children changes
 *   <li>A {@link DataListener} for node data changes (via TreeCache)
 * </ul>
 * <p>Handles both single-watch and recursive tree cache events.
 */
public class CuratorListenerImpl implements CuratorListener {
    protected static final Logger logger = LoggerFactory.getLogger(CuratorListenerImpl.class);

    private CuratorFramework client;
    private volatile ChildListener childListener;
    private String path;
    private volatile DataListener dataListener;

    protected CuratorListenerImpl() {
    }

    public CuratorListenerImpl(CuratorFramework client, ChildListener listener, String path) {
        this.client = client;
        this.childListener = listener;
        this.path = path;
    }

    public CuratorListenerImpl(CuratorFramework client, DataListener dataListener) {
        this.client = client;
        this.dataListener = dataListener;
    }

    @Override
    public void removeDataListener() {
        this.dataListener = null;
    }

    @Override
    public void removeChildrenListener() {
        this.childListener = null;
    }

    @Override
    public void process(WatchedEvent event) throws Exception {
        // if client connect or disconnect to server, zookeeper will queue
        // watched event(Watcher.Event.EventType.None, .., path = null).
        if (event.getType() == Watcher.Event.EventType.None) {
            return;
        }
        if (childListener != null) {
            childListener.childChanged(path, client.getChildren().usingWatcher(this).forPath(path));
        }
    }

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        if (dataListener != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("listen the zookeeper changed. The changed data:" + event.getData());
            }
            TreeCacheEvent.Type type = event.getType();
            EventType eventType = null;
            String content = null;
            String path = null;
            switch (type) {
                case NODE_ADDED:
                    eventType = EventType.NodeCreated;
                    path = event.getData().getPath();
                    content = event.getData().getData() == null ? "" : new String(event.getData().getData(), StandardCharsets.UTF_8);
                    break;
                case NODE_UPDATED:
                    eventType = EventType.NodeDataChanged;
                    path = event.getData().getPath();
                    content = event.getData().getData() == null ? "" : new String(event.getData().getData(), StandardCharsets.UTF_8);
                    break;
                case NODE_REMOVED:
                    path = event.getData().getPath();
                    eventType = EventType.NodeDeleted;
                    break;
                case INITIALIZED:
                    eventType = EventType.INITIALIZED;
                    break;
                case CONNECTION_LOST:
                    eventType = EventType.CONNECTION_LOST;
                    break;
                case CONNECTION_RECONNECTED:
                    eventType = EventType.CONNECTION_RECONNECTED;
                    break;
                case CONNECTION_SUSPENDED:
                    eventType = EventType.CONNECTION_SUSPENDED;
                    break;

            }
            dataListener.dataChanged(path, content, eventType);
        }
    }
}
