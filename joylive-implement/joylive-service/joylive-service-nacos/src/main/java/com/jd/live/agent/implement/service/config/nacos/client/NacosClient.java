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
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.nacos.NacosSyncKey;
import lombok.Getter;
import lombok.Setter;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.*;

/**
 * A client for interacting with the Nacos configuration service.
 */
public class NacosClient implements NacosClientApi {

    private static final Logger logger = LoggerFactory.getLogger(NacosClient.class);

    private static final Set<String> FORMATS = new HashSet<>(Arrays.asList("json", "properties", "yml", "yaml", "xml", "txt", "html", "toml"));

    private final NacosProperties properties;

    private final ObjectParser json;

    private ConfigService configService;

    private final Map<ConfigKey, ConfigWatcher> watchers = new ConcurrentHashMap<>();

    public NacosClient(NacosProperties properties, ObjectParser json) {
        this.properties = properties;
        this.json = json;
    }

    @Override
    public void connect() throws NacosException {
        Properties properties = new Properties();
        List<URI> uris = toList(split(this.properties.getUrl(), SEMICOLON_COMMA), URI::parse);
        String address = join(uris, uri -> uri.getAddress(true), CHAR_COMMA);
        properties.put(PropertyKeyConst.SERVER_ADDR, address);
        if (!isEmpty(this.properties.getNamespace()) && !DEFAULT_NAMESPACE_ID.equals(this.properties.getNamespace())) {
            properties.put(PropertyKeyConst.NAMESPACE, this.properties.getNamespace());
        }
        if (!isEmpty(this.properties.getUsername())) {
            properties.put(PropertyKeyConst.USERNAME, this.properties.getUsername());
            properties.put(PropertyKeyConst.PASSWORD, this.properties.getPassword());
        }
        configService = NacosFactory.createConfigService(properties);
    }

    @Override
    public void close() throws NacosException {
        if (configService != null) {
            configService.shutDown();
        }
    }

    @Override
    public void subscribe(String dataId, String group, Listener listener) throws NacosException {
        if (configService == null) {
            throw new NacosException(NacosException.CLIENT_DISCONNECT, "nacos is not connected.");
        }
        ConfigKey key = new ConfigKey(dataId, group);
        ConfigWatcher watcher = watchers.computeIfAbsent(key, k -> new ConfigWatcher(k, listener));
        watcher.subscribe();
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
         * Generates a beta variant of this configuration key by appending "-beta" suffix to the dataId.
         * The group remains unchanged.
         *
         * @return a new {@code ConfigKey} instance representing the beta configuration variant
         */
        public ConfigKey getBetaKey() {
            return new ConfigKey(getDataId("-beta", null), group);
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

        private Set<String> ips;

        private Map<String, String> labels;

        public boolean match() {
            return ips != null && !ips.isEmpty() && ips.contains(Ipv4.getLocalIp());
        }
    }

    /**
     * Watches for configuration changes in Nacos, handling both normal and beta configurations.
     * Automatically switches between release/beta configs based on policy matching.
     */
    private class ConfigWatcher {

        private final ConfigKey keyRelease;

        private final ConfigKey keyPolicy;

        private final ConfigKey keyBeta;

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
            this.keyBeta = key.getBetaKey();
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
                ConfigPolicy policy = value == null || value.isEmpty() ? null : json.read(new StringReader(value), ConfigPolicy.class);
                if (policy != null && policy.match()) {
                    if (beta.compareAndSet(false, true)) {
                        removeListener(keyRelease, onUpdate);
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
            configService.removeListener(key.getDataId(), key.getGroup(), listener);
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
}
