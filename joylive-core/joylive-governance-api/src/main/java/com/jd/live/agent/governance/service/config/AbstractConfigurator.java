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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.core.util.type.ValuePath;
import com.jd.live.agent.governance.subscription.config.ConfigEvent;
import com.jd.live.agent.governance.subscription.config.ConfigEvent.EventType;
import com.jd.live.agent.governance.subscription.config.ConfigListener;
import com.jd.live.agent.governance.subscription.config.Configurator;
import lombok.Getter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.jd.live.agent.governance.subscription.config.ConfigListener.SYSTEM_ALL;

/**
 * A configurator that uses Nacos as the configuration source.
 * <p>
 * This class implements the {@link Configurator} interface and provides methods for subscribing to configuration changes,
 * getting properties, adding and removing listeners, and publishing configuration events.
 */
public abstract class AbstractConfigurator<T extends ConfigClientApi> implements Configurator {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConfigurator.class);

    protected final List<ConfigSubscription<T>> subscriptions;

    protected final Map<String, NameListener> listeners = new ConcurrentHashMap<>();

    protected final AtomicReference<ConfigCache> ref = new AtomicReference<>();

    protected final AtomicBoolean started = new AtomicBoolean(false);

    protected final AtomicLong version = new AtomicLong(0);

    protected final Object mutex = new Object();

    protected int size;

    public AbstractConfigurator(List<ConfigSubscription<T>> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public void subscribe() throws Exception {
        if (started.compareAndSet(false, true)) {
            for (ConfigSubscription<T> subscription : subscriptions) {
                subscribe(subscription);
            }
        }
    }

    @Override
    public Object getProperty(String name) {
        ConfigCache cache = ref.get();
        if (cache == null || name == null || name.isEmpty()) {
            return null;
        }
        return new ValuePath(name).get(cache.getData());
    }

    @Override
    public void addListener(String name, ConfigListener listener) {
        if (name != null && !name.isEmpty() && listener != null) {
            SynchronousListener syncListener = new SynchronousListener(listener);
            listeners.computeIfAbsent(name, NameListener::new).addListener(syncListener);
            if (!SYSTEM_ALL.equals(name)) {
                long ver = version.get();
                Object value = getProperty(name);
                if (value != null) {
                    syncListener.onUpdate(new ConfigEvent(EventType.UPDATE, name, value, ver));
                }
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
     * Subscribes to the given configuration subscription.
     * <p>
     * Implementing classes should define the logic for handling the subscription.
     *
     * @param subscription The {@link ConfigSubscription} to subscribe to.
     * @throws Exception If an error occurs during the subscription process.
     */
    protected abstract void subscribe(ConfigSubscription<T> subscription) throws Exception;

    /**
     * Publishes a configuration event to all registered listeners for the specified property name.
     *
     * @param event The configuration event to be published.
     */
    protected void publish(ConfigEvent event) {
        NameListener listener = listeners.get(event.getName());
        if (listener != null) {
            listener.onUpdate(event);
        }
    }

    /**
     * Handles an update of the configuration information.
     *
     * @param configInfo   The updated configuration information.
     * @param subscription The configuration subscription associated with the update.
     */
    protected void onUpdate(String configInfo, ConfigSubscription<T> subscription) {
        logger.info("receive config update event from {}", subscription.getName().toString());
        ConfigParser parser = subscription.getParser();
        configInfo = configInfo == null ? "" : configInfo.trim();
        Map<String, Object> newValues = !configInfo.isEmpty() ? parser.parse(new StringReader(configInfo)) : new HashMap<>();
        onUpdate(newValues, subscription);
    }

    /**
     * Handles an update of the configuration information with parsed values.
     *
     * @param newValues    The parsed configuration values.
     * @param subscription The configuration subscription associated with the update.
     */
    protected void onUpdate(Map<String, Object> newValues, ConfigSubscription<T> subscription) {
        ConfigParser parser = subscription.getParser();
        Map<String, Object> newNestedValue = parser.isFlatted() ? CollectionUtils.cascade(newValues) : newValues;
        Map<String, Object> oldNestedValues = getNestedConfig(subscription);

        synchronized (mutex) {
            size = size + newNestedValue.size() - (oldNestedValues == null ? 0 : oldNestedValues.size());
            subscription.setConfig(newValues);

            Map<String, Object> newer;
            if (subscriptions.size() == 1) {
                newer = newNestedValue;
            } else {
                newer = new HashMap<>(size);
                for (ConfigSubscription<T> ns : subscriptions) {
                    Map<String, Object> config = getNestedConfig(ns);
                    if (config != null) {
                        newer.putAll(config);
                    }
                }
            }

            ConfigCache oldCache = ref.get();
            ConfigCache newCache = new ConfigCache(newer, version.incrementAndGet());
            ref.set(newCache);

            // Notify listeners based on their paths
            Map<String, Object> older = oldCache == null ? null : oldCache.getData();
            notifyListeners(older, newer, newCache.getVersion());
        }
    }

    protected void notifyListeners(Map<String, Object> older, Map<String, Object> newer, Long version) {
        int counter = 0;
        // For each listener, check if its path is affected by the changes
        for (NameListener listener : listeners.values()) {
            String name = listener.getName();
            if (SYSTEM_ALL.equals(name)) {
                continue;
            }

            Object newValue = listener.getPath().get(newer);
            Object oldValue = older != null ? listener.getPath().get(older) : null;

            if (newValue == null && oldValue != null) {
                counter++;
                publish(new ConfigEvent(EventType.DELETE, name, null, version));
            } else if (!Objects.equals(newValue, oldValue)) {
                counter++;
                publish(new ConfigEvent(EventType.UPDATE, name, newValue, version));
            }
        }

        if (counter > 0) {
            // Notify system-wide listeners
            publish(new ConfigEvent(EventType.UPDATE, SYSTEM_ALL, null, version));
        }
    }

    private Map<String, Object> getNestedConfig(ConfigSubscription<T> subscription) {
        Map<String, Object> config = subscription.getConfig();
        if (config != null && subscription.getParser().isFlatted()) {
            return CollectionUtils.cascade(config);
        }
        return config;
    }

    protected static class NameListener {

        @Getter
        private final String name;

        @Getter
        private final ValuePath path;

        private final List<SynchronousListener> listeners = new CopyOnWriteArrayList<>();

        NameListener(String name) {
            this.name = name;
            this.path = new ValuePath(name);
        }

        public void addListener(SynchronousListener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        public void removeIf(Predicate<SynchronousListener> predicate) {
            listeners.removeIf(predicate);
        }

        public boolean isEmpty() {
            return listeners.isEmpty();
        }

        public void onUpdate(ConfigEvent event) {
            for (SynchronousListener listener : listeners) {
                listener.onUpdate(event);
            }
        }
    }

    /**
     * A static inner class that implements the ConfigListener interface and provides synchronized access to the listener.
     */
    protected static class SynchronousListener implements ConfigListener {

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

    /**
     * A protected static class representing a cache for configuration data.
     * <p>
     * This class stores configuration data along with a version number to track updates.
     */
    @Getter
    protected static class ConfigCache {

        private final Map<String, Object> data;

        private final long version;

        ConfigCache(Map<String, Object> data, long version) {
            this.data = data;
            this.version = version;
        }

        public Object get(String key) {
            return data == null || key == null ? null : data.get(key);
        }
    }
}
