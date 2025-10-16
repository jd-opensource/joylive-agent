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
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.Locks;
import com.jd.live.agent.core.util.task.RetryExecution;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.config.nacos.client.converter.PropertiesConverter;
import com.jd.live.agent.implement.service.policy.nacos.NacosSyncKey;
import lombok.Getter;
import lombok.Setter;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

import static com.alibaba.nacos.ConnectionListener.LISTENER;

/**
 * A client for interacting with the Nacos configuration service.
 */
public class NacosClient extends AbstractNacosClient<NacosProperties, ConfigService> implements NacosClientApi {

    private static final Logger logger = LoggerFactory.getLogger(NacosClient.class);

    private static final Set<String> FORMATS = new HashSet<>(Arrays.asList("json", "properties", "yml", "yaml", "xml", "txt", "html", "toml"));

    private final ObjectParser json;
    private final Application application;

    private final Map<ConfigKey, ConfigWatcher> watchers = new ConcurrentHashMap<>();
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
        ConfigWatcher watcher = new ConfigWatcher(key, listener);
        Locks.write(lock, () -> {
            if (watchers.putIfAbsent(key, watcher) == null) {
                addTask(watcher, versions.get(), 0);
            }
        });

    }

    @Override
    public void unsubscribe(String dataId, String group, Listener listener) {
        ConfigKey key = new ConfigKey(dataId, group);
        Locks.write(lock, () -> {
            ConfigWatcher watcher = watchers.remove(key);
            if (watcher != null) {
                watcher.unsubscribe();
            }
        });

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
            for (Map.Entry<ConfigKey, ConfigWatcher> entry : watchers.entrySet()) {
                addTask(entry.getValue(), version, delay.get());
            }
        });

    }

    /**
     * Adds a config subscription task with version control.
     * @param watcher Configuration change listener
     * @param version Current config version for consistency
     */
    private void addTask(ConfigWatcher watcher, long version, long delay) {
        RetryVersionTimerTask.builder()
                .name("nacos.config.subscription")
                .task(new SubscriptionTask(watcher))
                .version(version)
                .predicate(predicate)
                .timer(timer)
                .build()
                .delay(delay);
    }

    /**
     * Represents a composite key for Nacos configuration items, consisting of a dataId and group pair.
     */
    @Getter
    private static class ConfigKey {

        private final String dataId;

        private final String group;

        ConfigKey(String dataId, String group) {
            this.dataId = dataId;
            this.group = group;
        }

        /**
         * Generates a policy variant of this configuration key by appending "-beta-policy" suffix
         * and ".json" extension to the dataId. The group remains unchanged.
         *
         * @return a new {@code ConfigKey} instance representing the policy configuration variant
         */
        public ConfigKey getPolicyKey() {
            return new ConfigKey(getDataId("-beta-policy", ".json"), group);
        }

        /**
         * Internal helper method to construct modified dataId with suffix and optional extension.
         * Handles special cases for existing file extensions in the original dataId.
         *
         * @param suffix    the suffix to append (e.g., "-beta", "-policy")
         * @param extension the file extension to apply (e.g., ".json"), may be null
         * @return modified dataId string combining original name with suffix and extension
         */
        private String getDataId(String suffix, String extension) {
            StringBuilder builder = new StringBuilder(dataId.length() + suffix.length());
            int pos = suffix.lastIndexOf('.');
            String ext = pos > 0 ? dataId.substring(pos).toLowerCase() : null;
            if (ext != null && FORMATS.contains(ext)) {
                builder.append(dataId, 0, pos);
                builder.append(suffix);
                builder.append(extension == null || extension.isEmpty() ? dataId.substring(pos + 1) : extension);
            } else {
                builder.append(dataId);
                builder.append(suffix);
                builder.append(extension == null ? "" : extension);
            }
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ConfigKey)) return false;
            ConfigKey that = (ConfigKey) o;
            return Objects.equals(dataId, that.dataId) && Objects.equals(group, that.group);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataId, group);
        }

    }

    /**
     * Represents a configuration policy with IP-based and label-based matching rules.
     */
    @Getter
    @Setter
    private static class ConfigPolicy {

        private String name;

        private Set<String> applications;

        private Set<String> ips;

        private Map<String, String> labels;

        public boolean match(Application application) {
            if (application == null) {
                return false;
            }
            String localIp = application.getLocation().getIp();
            if (applications != null && !applications.isEmpty() && !applications.contains(application.getName())) {
                return false;
            } else if (ips != null && !ips.isEmpty() && !ips.contains(localIp)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Watches for configuration changes in Nacos, handling both normal and beta configurations.
     * Automatically switches between release/beta configs based on policy matching.
     */
    private class ConfigWatcher {

        private static final int STATE_INIT = 0;

        private static final int STATE_BETA = 1;

        private static final int STATE_RELEASE = 2;

        private final ConfigKey keyRelease;

        private final ConfigKey keyPolicy;

        private ConfigKey keyBeta;

        private final Listener listener;

        private volatile String value;

        private final AtomicInteger state = new AtomicInteger(STATE_INIT);

        private final Listener onUpdate = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                onUpdate(configInfo);
            }
        };

        private final Listener onPolicy = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                try {
                    onPolicy(configInfo);
                } catch (NacosException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        };

        ConfigWatcher(ConfigKey key, Listener listener) {
            this.keyRelease = key;
            this.keyPolicy = key.getPolicyKey();
            this.listener = listener;
        }

        /**
         * Starts watching for config changes.
         *
         * @throws NacosException if initial setup fails
         */
        public void subscribe() throws NacosException {
            if (config.isGrayEnabled()) {
                logger.info("Subscribe gray policy {}@{}", keyPolicy.getDataId(), keyPolicy.getGroup());
                addListener(keyPolicy, onPolicy);
                onPolicy(getConfig(keyPolicy));
            } else {
                logger.info("Subscribe release config {}@{}", keyRelease.getDataId(), keyRelease.getGroup());
                addListener(keyRelease, onUpdate);
                onUpdate(getConfig(keyRelease));
            }
        }

        /**
         * Stops all config watches.
         */
        public void unsubscribe() {
            removeListener(keyRelease, onUpdate);
            if (config.isGrayEnabled()) {
                removeListener(keyPolicy, onPolicy);
                removeListener(keyBeta, onUpdate);
            }
        }

        /**
         * Resubscribes by resetting state and recreating subscription.
         * Unsubscribes first to avoid duplicate listeners.
         *
         * @throws NacosException if subscription fails
         */
        public void resubscribe() throws NacosException {
            // Since connection recovery keeps the state, we need to initialize the state
            state.set(STATE_INIT);
            // unsubscribe first to avoid add duplicated listeners.
            unsubscribe();
            subscribe();
        }

        /**
         * Handles policy config updates.
         *
         * @param value New policy config JSON
         * @throws NacosException if policy processing fails
         */
        private synchronized void onPolicy(String value) throws NacosException {
            try {
                List<ConfigPolicy> policies = value == null || value.isEmpty() ? null : json.read(new StringReader(value), new TypeReference<List<ConfigPolicy>>() {
                });
                ConfigPolicy policy = null;
                if (policies != null && !policies.isEmpty()) {
                    for (ConfigPolicy p : policies) {
                        if (p.match(application)) {
                            policy = p;
                            break;
                        }
                    }
                }
                if (policy != null) {
                    ConfigKey newKeyBeta = new ConfigKey(policy.getName(), keyRelease.getGroup());
                    if (state.compareAndSet(STATE_RELEASE, STATE_BETA)
                            || state.compareAndSet(STATE_INIT, STATE_BETA)
                            || state.get() == STATE_BETA && !newKeyBeta.equals(keyBeta)) {
                        logger.info("Subscribe gray config {}@{}", keyBeta.getDataId(), keyBeta.getGroup());
                        removeListener(keyRelease, onUpdate);
                        removeListener(keyBeta, onUpdate);
                        keyBeta = newKeyBeta;
                        addListener(keyBeta, onUpdate);
                        onUpdate(getConfig(keyBeta));
                    }
                } else if (state.compareAndSet(STATE_BETA, STATE_RELEASE)
                        || state.compareAndSet(STATE_INIT, STATE_RELEASE)) {
                    logger.info("Subscribe release config {}@{}", keyRelease.getDataId(), keyRelease.getGroup());
                    removeListener(keyBeta, onUpdate);
                    addListener(keyRelease, onUpdate);
                    onUpdate(getConfig(keyRelease));
                }
            } catch (NacosException e) {
                throw e;
            } catch (Throwable e) {
                throw new NacosException(NacosException.CLIENT_ERROR, e.getMessage(), e);
            }
        }

        private void addListener(ConfigKey key, Listener listener) throws NacosException {
            client.addListener(key.getDataId(), key.getGroup(), listener);
        }

        private void removeListener(ConfigKey key, Listener listener) {
            if (key != null && listener != null) {
                client.removeListener(key.getDataId(), key.getGroup(), listener);
            }
        }

        private String getConfig(ConfigKey key) throws NacosException {
            return client.getConfig(key.getDataId(), key.getGroup(), config.getTimeout());
        }

        /**
         * Handles config value updates.
         *
         * @param value New configuration value
         */
        private synchronized void onUpdate(String value) {
            if (!Objects.equals(this.value, value)) {
                this.value = value;
                listener.receiveConfigInfo(value);
            }
        }
    }

    /**
     * Retry task for failed Nacos config subscriptions.
     */
    private class SubscriptionTask implements RetryExecution {

        private final ConfigWatcher watcher;

        SubscriptionTask(ConfigWatcher watcher) {
            this.watcher = watcher;
        }

        @Override
        public Boolean call() throws Exception {
            if (watchers.get(watcher.keyRelease) == watcher) {
                try {
                    // retry on exception
                    watcher.resubscribe();
                } catch (NacosException e) {
                    logger.error("Failed to subscribe {}, retry later, caused by {}", watcher.keyRelease.dataId, e.getMessage());
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
