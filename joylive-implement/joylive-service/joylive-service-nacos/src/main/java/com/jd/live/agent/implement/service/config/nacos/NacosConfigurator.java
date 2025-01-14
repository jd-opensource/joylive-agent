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

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NacosConfigurator implements Configurator {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfigurator.class);

    private final NacosClientApi client;

    private final ConfigName configName;

    private final ConfigParser parser;

    private final Map<String, List<ConfigListener>> listeners = new ConcurrentHashMap<>();

    private final AtomicReference<Map<String, Object>> ref = new AtomicReference<>();

    private final Object mutex = new Object();

    private final AtomicBoolean started = new AtomicBoolean(false);

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
        Map<String, Object> map = ref.get();
        return map == null ? null : map.get(name);
    }

    @Override
    public void addListener(String name, ConfigListener listener) {
        if (name != null && !name.isEmpty() && listener != null) {
            listeners.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(listener);
            publish(listener);
        }
    }

    @Override
    public void removeListener(String name, ConfigListener listener) {
        if (name != null && !name.isEmpty() && listener != null) {
            listeners.computeIfPresent(name, (k, v) -> {
                v.remove(listener);
                return v.isEmpty() ? null : v;
            });
        }
    }

    private void publish(ConfigEvent event) {
        synchronized (mutex) {
            List<ConfigListener> watchers = listeners.get(event.getName());
            if (watchers != null) {
                for (ConfigListener listener : watchers) {
                    listener.onUpdate(event);
                }
            }
        }
    }

    private void publish(ConfigListener listener) {
        synchronized (mutex) {
            Map<String, Object> map = ref.get();
            if (map != null) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    ConfigEvent event = ConfigEvent.builder().name(entry.getKey()).value(entry.getValue()).type(EventType.UPDATE).build();
                    listener.onUpdate(event);
                }
            }
        }
    }

    private void onDelete(Map<String, Object> older, Map<String, Object> newer) {
        if (older != null) {
            for (Map.Entry<String, Object> entry : older.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (!newer.containsKey(key)) {
                    ConfigEvent event = ConfigEvent.builder().type(EventType.DELETE).name(key).value(value).build();
                    publish(event);
                }
            }
        }
    }

    private void onUpdate(Map<String, Object> newer, Map<String, Object> older) {
        if (newer != null) {
            for (Map.Entry<String, Object> entry : newer.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Object old = older == null ? null : older.get(key);
                if (value != null && !value.equals(old)) {
                    ConfigEvent event = ConfigEvent.builder().type(EventType.UPDATE).name(key).value(value).build();
                    publish(event);
                }
            }
        }
    }

    private class ChangeListener implements Listener {
        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public void receiveConfigInfo(String configInfo) {
            configInfo = configInfo == null ? "" : configInfo.trim();
            Map<String, Object> newer = null;
            if (!configInfo.isEmpty()) {
                newer = parser.parse(new StringReader(configInfo));
            }
            Map<String, Object> older = ref.getAndSet(newer);
            onUpdate(newer, older);
            onDelete(older, newer);
        }
    }
}
