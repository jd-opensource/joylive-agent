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
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.Executors;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.task.RetryVersionTask;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.probe.DetectTaskListener;
import com.jd.live.agent.governance.probe.FailoverDetectTask;
import com.jd.live.agent.governance.probe.FailoverRecoverTask;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.nacos.NacosSyncKey;
import lombok.Getter;
import lombok.Setter;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;
import static com.alibaba.nacos.client.config.impl.ConnectionListener.LISTENER;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.*;

/**
 * A client for interacting with the Nacos configuration service.
 */
public class NacosClient implements NacosClientApi {

    private static final Logger logger = LoggerFactory.getLogger(NacosClient.class);

    private static final Set<String> FORMATS = new HashSet<>(Arrays.asList("json", "properties", "yml", "yaml", "xml", "txt", "html", "toml"));

    private final NacosProperties properties;
    private final HealthProbe probe;
    private final Timer timer;
    private final ObjectParser json;
    private final Application application;

    private final long timeout;
    private final boolean autoRecover;
    private final List<String> servers;
    private String server;
    private final NacosFailoverAddressList addressList;
    private final Properties config = new Properties();

    private volatile ConfigService configService;
    private final Map<ConfigKey, ConfigWatcher> watchers = new ConcurrentHashMap<>();
    private final CountDownLatch connectLatch = new CountDownLatch(1);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong versions = new AtomicLong(0);
    private final Predicate<RetryVersionTask> predicate = p -> started.get() && p.getVersion() == versions.get();

    public NacosClient(NacosProperties properties, HealthProbe probe, Timer timer, ObjectParser json, Application application) {
        this.properties = properties;
        this.probe = probe;
        this.timer = timer;
        this.json = json;
        this.application = application;
        this.timeout = properties.getTimeout() <= 0 ? 5000 : properties.getTimeout();
        this.autoRecover = MapOption.of(properties.getProperties()).getBoolean("autoRecover", true);
        this.servers = toList(splitList(properties.getUrl(), CHAR_SEMICOLON),
                v -> join(toList(toList(splitList(v, CHAR_COMMA), URI::parse), u -> u.getAddress(true))));
        this.addressList = new NacosFailoverAddressList(servers);
        if (properties.getProperties() != null) {
            config.putAll(properties.getProperties());
        }
        if (!isEmpty(properties.getNamespace()) && !DEFAULT_NAMESPACE_ID.equals(properties.getNamespace())) {
            config.put(PropertyKeyConst.NAMESPACE, properties.getNamespace());
        }
        if (!isEmpty(properties.getUsername())) {
            config.put(PropertyKeyConst.USERNAME, properties.getUsername());
            config.put(PropertyKeyConst.PASSWORD, properties.getPassword());
        }

    }

    @Override
    public void connect() throws NacosException {
        if (started.compareAndSet(false, true)) {
            logger.info("Try detecting healthy nacos {}", join(servers));
            try {
                // wait for connected
                addDetectTask(0);
                if (!connectLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                    logger.error("It's timeout to connect to nacos. {}", join(servers));
                    // cancel task.
                    throw new NacosException(NacosException.CLIENT_DISCONNECT, "It's timeout to connect to nacos.");
                }
            } catch (NacosException e) {
                started.set(false);
                throw e;
            } catch (InterruptedException e) {
                started.set(false);
                Thread.currentThread().interrupt();
                throw new NacosException(NacosException.CLIENT_DISCONNECT, "The nacos connecting thread is interrupted.");
            }
        }

    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            connected.set(false);
            close(configService);
        }
    }

    @Override
    public void subscribe(String dataId, String group, Listener listener) throws NacosException {
        ConfigKey key = new ConfigKey(dataId, group);
        ConfigWatcher watcher = watchers.computeIfAbsent(key, k -> new ConfigWatcher(k, listener));
        if (connected.get()) {
            watcher.subscribe();
        }
    }

    @Override
    public void unsubscribe(String dataId, String group, Listener listener) {
        if (configService != null) {
            ConfigKey key = new ConfigKey(dataId, group);
            ConfigWatcher watcher = watchers.remove(key);
            if (watcher != null) {
                watcher.unsubscribe();
            }
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

    /**
     * Schedules a new failover detection task with version control.
     *
     * @param delay Initial delay before first execution (milliseconds)
     */
    private void addDetectTask(long delay) {
        long version = versions.incrementAndGet();
        FailoverDetectTask detect = new FailoverDetectTask(addressList, probe, 1, false, new NacosDetectTaskListener());
        RetryVersionTimerTask task = new RetryVersionTimerTask("nacos.detect", detect, version, predicate, timer);
        // fast to reconnect when initialization
        task.delay(delay);
    }

    /**
     * Gracefully shuts down a ConfigService instance.
     *
     * @param service The ConfigService to close
     */
    private void close(ConfigService service) {
        if (service != null) {
            try {
                service.shutDown();
            } catch (NacosException ignored) {
            }
        }
    }

    /**
     * Handles disconnection by closing current config service
     * and immediately scheduling a failover detection task.
     */
    private void onDisconnected() {
        connected.set(false);
        close(configService);
        addDetectTask(Timer.getRetryInterval(1000, 3000L));
    }

    /**
     * Reconnects to the current Nacos server address.
     *
     * @param address The address to connect to
     */
    private void onDetected(String address) {
        if (!address.equals(server)) {
            config.put(PropertyKeyConst.SERVER_ADDR, address);
            // set listener thread local to add it in ServerListManager
            LISTENER.set(this::onDisconnected);
            try {
                // re-create config service
                configService = Executors.call(NacosClient.class.getClassLoader(), () -> NacosFactory.createConfigService(config));
                server = address;
                resubscribe();
                connected.set(true);
                connectLatch.countDown();
                doRecover(address);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to connect to nacos " + address, e);
            } finally {
                LISTENER.remove();
            }
        }
    }

    /**
     * Resubscribes all registered config watchers.
     */
    private void resubscribe() {
        ConfigWatcher watcher;
        for (Map.Entry<ConfigKey, ConfigWatcher> entry : watchers.entrySet()) {
            watcher = entry.getValue();
            try {
                watcher.subscribe();
            } catch (NacosException e) {
                ResubscribeTask resubscribe = new ResubscribeTask(watcher);
                resubscribe.addTask();
            }
        }
    }

    /**
     * Attempts to detect and recover connection to the preferred nacos server.
     *
     * @param address The current address.
     */
    private void doRecover(String address) {
        if (!autoRecover) {
            return;
        }
        String first = addressList.first();
        if (Objects.equals(address, first)) {
            return;
        }
        logger.info("Try detecting unhealthy preferred nacos {}...", first);
        FailoverRecoverTask execution = new FailoverRecoverTask(first, probe, 1, () -> {
            if (!Objects.equals(addressList.current(), first)) {
                // recover immediately
                connected.set(false);
                close(configService);
                // reset preferred nacos
                addressList.reset();
                logger.info("Try switching to the healthy preferred nacos {}.", addressList.current());
                onDetected(addressList.current());
            }
        });
        RetryVersionTimerTask task = new RetryVersionTimerTask("nacos.recover", execution, versions.get(), predicate, timer);
        task.delay(Timer.getRetryInterval(1500, 5000));
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

        private final ConfigKey keyRelease;

        private final ConfigKey keyPolicy;

        private ConfigKey keyBeta;

        private final Listener listener;

        private volatile String value;

        private final AtomicBoolean beta = new AtomicBoolean(false);

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
            if (properties.isGrayEnabled()) {
                addListener(keyPolicy, onPolicy);
                onPolicy(getConfig(keyPolicy));
            } else {
                addListener(keyRelease, onUpdate);
                onUpdate(getConfig(keyRelease));
            }
        }

        /**
         * Stops all config watches.
         */
        public void unsubscribe() {
            removeListener(keyRelease, onUpdate);
            if (properties.isGrayEnabled()) {
                removeListener(keyPolicy, onPolicy);
                removeListener(keyBeta, onUpdate);
            }
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
                    if (beta.compareAndSet(false, true) || !newKeyBeta.equals(keyBeta)) {
                        removeListener(keyRelease, onUpdate);
                        removeListener(keyBeta, onUpdate);
                        keyBeta = newKeyBeta;
                        addListener(keyBeta, onUpdate);
                        onUpdate(getConfig(keyBeta));
                    }
                } else if (beta.compareAndSet(true, false)) {
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
            configService.addListener(key.getDataId(), key.getGroup(), listener);
        }

        private void removeListener(ConfigKey key, Listener listener) {
            if (key != null && listener != null) {
                configService.removeListener(key.getDataId(), key.getGroup(), listener);
            }
        }

        private String getConfig(ConfigKey key) throws NacosException {
            return configService.getConfig(key.getDataId(), key.getGroup(), properties.getTimeout());
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
     * Listener for Nacos server detection tasks.
     * Handles connection success/failure events and manages ConfigService lifecycle.
     */
    private class NacosDetectTaskListener implements DetectTaskListener {
        @Override
        public void onSuccess() {
            String current = addressList.current();
            if (!current.equals(server)) {
                logger.info("Try connecting to healthy nacos {}", current);
                // reconnect to server
                onDetected(current);
            }
        }

        @Override
        public void onFailure() {
            connectLatch.countDown();
        }
    }

    /**
     * Retry task for failed Nacos config subscriptions.
     */
    private class ResubscribeTask implements Runnable {

        private final ConfigWatcher watcher;

        ResubscribeTask(ConfigWatcher watcher) {
            this.watcher = watcher;
        }

        @Override
        public void run() {
            if (started.get() && watchers.containsKey(watcher.keyRelease)) {
                try {
                    watcher.subscribe();
                } catch (NacosException e) {
                    addTask();
                }
            }
        }

        public void addTask() {
            timer.delay("resubscribe-config", Timer.getRetryInterval(1000L, 1000L), this);
        }
    }

}
