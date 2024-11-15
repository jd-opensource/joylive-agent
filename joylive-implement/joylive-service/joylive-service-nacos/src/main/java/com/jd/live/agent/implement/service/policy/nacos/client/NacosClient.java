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
package com.jd.live.agent.implement.service.policy.nacos.client;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;

import java.util.Properties;
import java.util.function.Function;

/**
 * A client for interacting with the Nacos configuration service.
 */
public class NacosClient implements AutoCloseable {

    private final NacosSyncConfig config;

    private ConfigService configService;

    public NacosClient(NacosSyncConfig config) {
        this.config = config;
    }

    /**
     * Connects to the Nacos server using the specified configuration.
     *
     * @throws NacosException If there is an error connecting to the Nacos server.
     */
    public void connect() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, config.getServerAddr());
        properties.put(PropertyKeyConst.NAMESPACE, config.getNamespace());
        properties.put(PropertyKeyConst.USERNAME, config.getUsername());
        properties.put(PropertyKeyConst.PASSWORD, config.getPassword());
        configService = NacosFactory.createConfigService(properties);
    }

    @Override
    public void close() throws NacosException {
        if (configService != null) {
            configService.shutDown();
        }
    }

    /**
     * Subscribes to configuration changes for the specified dataId and group.
     *
     * @param dataId   The dataId of the configuration to subscribe to.
     * @param group    The group of the configuration to subscribe to.
     * @param listener The listener to notify when configuration changes occur.
     * @throws NacosException If there is an error subscribing to the configuration changes.
     */
    public void subscribe(String dataId, String group, Listener listener) throws NacosException {
        if (configService == null) {
            throw new NacosException(NacosException.CLIENT_DISCONNECT, "nacos is not connected.");
        }
        String cfg = configService.getConfig(dataId, group, config.getTimeout());
        listener.receiveConfigInfo(cfg);
        configService.addListener(dataId, group, listener);
    }

    /**
     * Unsubscribes from configuration changes for the specified dataId and group.
     *
     * @param dataId   The dataId of the configuration to unsubscribe from.
     * @param group    The group of the configuration to unsubscribe from.
     * @param listener The listener to remove from the subscription.
     */
    public void unsubscribe(String dataId, String group, Listener listener) {
        if (configService != null) {
            configService.removeListener(dataId, group, listener);
        }
    }

    /**
     * Creates a new Syncer object that can be used to synchronize data between Nacos and a local cache.
     * @param parser A function that takes a configuration string as input and returns an object of type T.
     * @param <K> The type of the NacosSyncKey used to identify the configuration in Nacos.
     * @param <T> The type of the object returned by the parser function.
     * @return A new Syncer object that can be used to synchronize data between Nacos and a local cache.
     */
    public <K extends NacosSyncKey, T> Syncer<K, T> createSyncer(Function<String, SyncResponse<T>> parser) {
        return subscription -> {
            try {
                subscribe(subscription.getKey().getDataId(), subscription.getKey().getGroup(), new AbstractListener() {
                    // TODO executor
                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        subscription.onUpdate(parser.apply(configInfo));
                    }
                });
            } catch (Throwable e) {
                subscription.onUpdate(new SyncResponse<>(e));
            }
        };
    }
}
