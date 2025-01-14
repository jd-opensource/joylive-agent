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
package com.jd.live.agent.implement.service.config.nacos;

import com.alibaba.nacos.api.config.listener.Listener;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.config.ConfigListener;
import com.jd.live.agent.core.config.ConfigName;
import com.jd.live.agent.core.config.Configurator;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientApi;
import lombok.Getter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A configurator that uses Nacos as the configuration source.
 * <p>
 * This class implements the {@link Configurator} interface and provides methods for subscribing to configuration changes,
 * getting properties, adding and removing listeners, and publishing configuration events.
 */
public class NacosConfigurator implements Configurator {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfigurator.class);

    private final NacosClientApi client;

    private final ConfigName configName;

    private final ConfigParser parser;

    private final Map<String, List<SynchronousListener>> listeners = new ConcurrentHashMap<>();

    private final AtomicReference<ConfigCache> ref = new AtomicReference<>();

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicLong version = new AtomicLong(0);

    public NacosConfigurator(NacosClientApi client, ConfigName configName, ConfigParser parser) {
        this.client = client;
        this.configName = configName;
        this.parser = parser;
    }

    @Override
    public String getName() {
        return "nacos";
    }

    @Override
    public void subscribe() throws Exception {
        if (started.compareAndSet(false, true)) {
            logger.info("subscribe " + configName + ", parser " + parser.getClass().getSimpleName());
            client.subscribe(configName.getName(), configName.getProfile(), new ChangeListener());
        }
    }

    @Override
    public Object getProperty(String name) {
        ConfigCache cache = ref.get();
        return cache == null ? null : cache.get(name);
    }

    @Override
    public void addListener(String name, ConfigListener listener) {
        if (name != null && !name.isEmpty() && listener != null) {
            SynchronousListener syncListener = new SynchronousListener(listener);
            listeners.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(syncListener);
            long ver = version.get();
            Object value = getProperty(name);
            if (value != null) {
                syncListener.onUpdate(ConfigEvent.builder()
                        .name(name)
                        .value(value)
                        .type(EventType.UPDATE)
                        .version(ver)
                        .build());
            }
        }
    }

    @Override
    public void removeListener(String name, ConfigListener listener) {
        if (name != null && !name.isEmpty() && listener != null) {
            listeners.computeIfPresent(name, (k, v) -> {
                v.removeIf(t -> t.listener == listener);
                return v.isEmpty() ? null : v;
            });
        }
    }

    /**
     * Publishes a configuration event to all registered listeners for the specified property name.
     *
     * @param event The configuration event to be published.
     */
    private void publish(ConfigEvent event) {
        List<SynchronousListener> watchers = listeners.get(event.getName());
        if (watchers != null) {
            for (ConfigListener listener : watchers) {
                listener.onUpdate(event);
            }
        }
    }

    /**
     * Handles an update of the configuration information.
     *
     * @param configInfo The updated configuration information.
     */
    private void onUpdate(String configInfo) {
        configInfo = configInfo == null ? "" : configInfo.trim();
        Map<String, Object> newer = !configInfo.isEmpty() ? parser.parse(new StringReader(configInfo)) : new HashMap<>();
        ConfigCache oldCache = ref.get();
        ConfigCache newCache = new ConfigCache(newer, version.incrementAndGet());
        ref.set(newCache);
        Map<String, Object> older = oldCache == null ? null : oldCache.getData();
        onUpdate(newer, older, newCache.getVersion());
        onDelete(older, newer, newCache.getVersion());
    }

    /**
     * Handles the deletion of properties from the configuration.
     *
     * @param older   The older configuration.
     * @param newer   The newer configuration.
     * @param version The version number of the configuration.
     */
    private void onDelete(Map<String, Object> older, Map<String, Object> newer, long version) {
        if (older != null) {
            for (Map.Entry<String, Object> entry : older.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (!newer.containsKey(key)) {
                    publish(ConfigEvent.builder()
                            .type(EventType.DELETE)
                            .name(key)
                            .value(value)
                            .version(version)
                            .build());
                }
            }
        }
    }

    /**
     * Handles the update of properties in the configuration.
     *
     * @param newer   The newer configuration.
     * @param older   The older configuration.
     * @param version The version number of the configuration.
     */
    private void onUpdate(Map<String, Object> newer, Map<String, Object> older, long version) {
        if (newer != null) {
            for (Map.Entry<String, Object> entry : newer.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Object old = older == null ? null : older.get(key);
                if (value != null && !value.equals(old)) {
                    publish(ConfigEvent.builder()
                            .type(EventType.UPDATE)
                            .name(key)
                            .value(value)
                            .version(version)
                            .build());
                }
            }
        }
    }

    /**
     * An inner class that implements the Listener interface and handles configuration changes.
     */
    private class ChangeListener implements Listener {
        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public void receiveConfigInfo(String configInfo) {
            onUpdate(configInfo);
        }
    }

    /**
     * A static inner class that implements the ConfigListener interface and provides synchronized access to the listener.
     */
    private static class SynchronousListener implements ConfigListener {

        private final ConfigListener listener;

        private final Object mutex = new Object();

        private volatile long version = -1;

        SynchronousListener(ConfigListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean onUpdate(ConfigEvent event) {
            synchronized (mutex) {
                if (event.getVersion() > version) {
                    version = event.getVersion();
                    return listener.onUpdate(event);
                }
                return false;
            }
        }
    }

    @Getter
    private static class ConfigCache {

        private final Map<String, Object> data;

        private final long version;

        ConfigCache(Map<String, Object> data, long version) {
            this.data = data;
            this.version = version;
        }

        public Object get(String key) {
            // TODO handle yaml?
            return data == null || key == null ? null : data.get(key);
        }
    }
}
