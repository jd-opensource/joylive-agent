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
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.nacos.NacosSyncKey;

import java.util.List;
import java.util.Properties;
import java.util.function.BiFunction;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.*;

/**
 * A client for interacting with the Nacos configuration service.
 */
public class NacosClient implements NacosClientApi {

    private final NacosProperties config;

    private ConfigService configService;

    public NacosClient(NacosProperties config) {
        this.config = config;
    }

    @Override
    public void connect() throws NacosException {
        Properties properties = new Properties();
        List<URI> uris = toList(split(config.getUrl(), SEMICOLON_COMMA), URI::parse);
        String address = join(uris, uri -> uri.getAddress(true), CHAR_COMMA);
        properties.put(PropertyKeyConst.SERVER_ADDR, address);
        if (!isEmpty(config.getNamespace()) && !DEFAULT_NAMESPACE_ID.equals(config.getNamespace())) {
            properties.put(PropertyKeyConst.NAMESPACE, config.getNamespace());
        }
        if (!isEmpty(config.getUsername())) {
            properties.put(PropertyKeyConst.USERNAME, config.getUsername());
            properties.put(PropertyKeyConst.PASSWORD, config.getPassword());
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
        String cfg = configService.getConfig(dataId, group, config.getTimeout());
        listener.receiveConfigInfo(cfg);
        configService.addListener(dataId, group, listener);
    }

    @Override
    public void unsubscribe(String dataId, String group, Listener listener) {
        if (configService != null) {
            configService.removeListener(dataId, group, listener);
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
}
