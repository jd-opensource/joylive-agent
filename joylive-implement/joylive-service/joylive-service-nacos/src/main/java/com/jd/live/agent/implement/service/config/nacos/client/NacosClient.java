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
package com.jd.live.agent.implement.service.config.nacos.client;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Locks;
import com.jd.live.agent.core.util.task.RetryExecution;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.governance.subscription.config.gray.ConfigFetcher;
import com.jd.live.agent.governance.subscription.config.gray.ConfigKey;
import com.jd.live.agent.governance.subscription.config.gray.ConfigWatcher;
import com.jd.live.agent.implement.service.config.nacos.client.converter.PropertiesConverter;
import com.jd.live.agent.implement.service.policy.nacos.NacosSyncKey;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.alibaba.nacos.ConnectionListener.LISTENER;

/**
 * A client for interacting with the Nacos configuration service.
 */
public class NacosClient extends AbstractNacosClient<NacosProperties, ConfigService> implements NacosClientApi {

    private static final Logger logger = LoggerFactory.getLogger(NacosClient.class);

    private final ObjectParser json;

    private final Application application;

    private final Map<ConfigKey, NacosConfigWatcher> watchers = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public NacosClient(NacosProperties properties, HealthProbe probe, Timer timer, ObjectParser json, Application application) {
        super(properties, probe, timer);
        this.json = json;
        this.application = application;
    }

    @Override
    protected String getAddress(NacosProperties config) {
        return config.getUrl();
    }

    @Override
    protected Properties convert(NacosProperties config) {
        return PropertiesConverter.INSTANCE.convert(config);
    }

    @Override
    public void connect() throws NacosException {
        doStart();
    }

    @Override
    public void close() throws Exception {
        doClose();
    }

    @Override
    public void subscribe(String dataId, String group, Listener listener) {
        ConfigKey key = new ConfigKey(dataId, group);
        Locks.write(lock, () -> {
            watchers.compute(key, (k, v) -> {
                if (v == null) {
                    v = new NacosConfigWatcher(client, key, application, json);
                    v.addListener(listener);
                    addTask(v, versions.get(), 0);
                } else {
                    v.addListener(listener);
                }
                return v;
            });
        });

    }

    @Override
    public void unsubscribe(String dataId, String group, Listener listener) {
        ConfigKey key = new ConfigKey(dataId, group);
        AtomicReference<NacosConfigWatcher> ref = new AtomicReference<>();
        Locks.write(lock, () -> {
            watchers.computeIfPresent(key, (k, v) -> {
                if (v.removeListener(listener) && v.isEmpty()) {
                    ref.set(v);
                    v = null;
                }
                return v;
            });
        });
        NacosConfigWatcher watcher = ref.get();
        if (watcher != null) {
            watcher.unsubscribe();
        }
    }

    @Override
    public <K extends NacosSyncKey, T> Syncer<K, T> createSyncer(BiFunction<K, String, SyncResponse<T>> parser) {
        return subscription -> {
            try {
                K key = subscription.getKey();
                subscribe(key.getDataId(), key.getGroup(), new AbstractListener() {
                    // TODO executor
                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        subscription.onUpdate(parser.apply(key, configInfo));
                    }
                });
            } catch (Throwable e) {
                subscription.onUpdate(new SyncResponse<>(e));
            }
        };
    }

    @Override
    protected void close(ConfigService client) {
        if (client != null) {
            try {
                client.shutDown();
            } catch (NacosException ignored) {
            }
        }
    }

    @Override
    protected void reconnect(String address) {
        // set listener thread local to add it in ServerListManager
        LISTENER.set(this::onDisconnected);
        try {
            doReconnect(address);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect to nacos " + address, e);
        } finally {
            LISTENER.remove();
        }
    }

    @Override
    protected ConfigService createClient() throws NacosException {
        return NacosFactory.createConfigService(properties);
    }

    @Override
    protected void recover() {
        // resubscribe watcher
        long version = versions.get();
        Locks.read(lock, () -> {
            for (Map.Entry<ConfigKey, NacosConfigWatcher> entry : watchers.entrySet()) {
                addTask(entry.getValue(), version, delay.get());
            }
        });

    }

    /**
     * Adds a config subscription task with version control.
     * @param watcher Configuration change listener
     * @param version Current config version for consistency
     */
    private void addTask(NacosConfigWatcher watcher, long version, long delayMs) {
        RetryVersionTimerTask.builder()
                .name("nacos.config.subscription")
                .task(new SubscriptionTask(watcher, this::exists, delay))
                .version(version)
                .predicate(predicate)
                .timer(timer)
                .build()
                .delay(delayMs);
    }

    /**
     * Check the existence of a watcher in the watchers map.
     *
     * @param watcher the watcher to check
     * @return true if the watcher exists, false otherwise
     */
    private boolean exists(NacosConfigWatcher watcher) {
        return watchers.get(watcher.getKeyRelease()) == watcher;
    }

    /**
     * Nacos configuration fetcher that retrieves configuration data from Nacos server.
     */
    private static class NacosConfigFetcher extends ConfigFetcher<ConfigService> {

        NacosConfigFetcher(ConfigService client, ConfigKey key, Application application, ObjectParser json) {
            super(client, key, application, json);
        }

        @Override
        protected String doGetConfig(ConfigKey key, long timeout) throws Exception {
            return getClient().getConfig(key.getName(), key.getGroup(), timeout);
        }
    }

    /**
     * Watches for configuration changes in Nacos, handling both normal and beta configurations.
     * Automatically switches between release/beta configs based on policy matching.
     */
    private class NacosConfigWatcher extends ConfigWatcher<ConfigService, Listener> {

        NacosConfigWatcher(ConfigService client, ConfigKey key, Application application, ObjectParser json) {
            super(client, key, application, json);
        }

        @Override
        public ConfigService getClient() {
            return NacosClient.this.client;
        }

        @Override
        protected Listener createOnPolicyListener() {
            return new AbstractListener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    try {
                        onUpdatePolicy(configInfo);
                    } catch (Exception e) {
                        logger.error("Failed to update policy {}, caused by {}", keyPolicy, e.getMessage(), e);
                    }
                }
            };
        }

        @Override
        protected Listener createOnUpdateListener() {
            return new AbstractListener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    onUpdateConfig(configInfo);
                }
            };
        }

        @Override
        protected void doSubscribe(ConfigKey key, Listener listener) throws Exception {
            getClient().addListener(key.getName(), key.getGroup(), listener);
        }

        @Override
        protected void doUnsubscribe(ConfigKey key, Listener listener) {
            getClient().removeListener(key.getName(), key.getGroup(), listener);
        }

        @Override
        protected String doGetConfig(ConfigKey key, long timeout) throws Exception {
            return getClient().getConfig(key.getName(), key.getGroup(), timeout);
        }

        @Override
        protected void doUpdateConfig(Listener listener, String value) {
            listener.receiveConfigInfo(value);
        }
    }

    /**
     * Retry task for failed Nacos config subscriptions.
     */
    private static class SubscriptionTask implements RetryExecution {

        private final NacosConfigWatcher watcher;

        private final Predicate<NacosConfigWatcher> predicate;

        private final Supplier<Long> delay;

        SubscriptionTask(NacosConfigWatcher watcher, Predicate<NacosConfigWatcher> predicate, Supplier<Long> delay) {
            this.watcher = watcher;
            this.predicate = predicate;
            this.delay = delay;
        }

        @Override
        public Boolean call() throws Exception {
            ConfigKey key = watcher.getKeyRelease();
            if (predicate.test(watcher)) {
                try {
                    // retry on exception
                    watcher.resubscribe();
                    // check if watcher is removed
                    if (!predicate.test(watcher)) {
                        watcher.unsubscribe();
                    }
                } catch (NacosException e) {
                    logger.error("Failed to subscribe {}, retry later, caused by {}", key.getName(), e.getMessage());
                    throw e;
                }
            }
            return true;
        }

        @Override
        public long getRetryInterval() {
            return delay.get();
        }
    }

}
