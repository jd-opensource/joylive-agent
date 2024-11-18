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

import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.nacos.NacosSyncKey;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosConfig;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class NacosClientFactory {

    private static final Map<String, SharedNacosClientApi> clients = new ConcurrentHashMap<>();

    /**
     * Creates a new instance of NacosClientApi based on the provided NacosSyncConfig.
     *
     * @param config The NacosSyncConfig object to use for creating the NacosClientApi instance.
     * @return A new instance of NacosClientApi based on the provided NacosSyncConfig.
     */
    public static NacosClientApi create(NacosSyncConfig config) {
        NacosConfig nacos = config.getNacos();
        URI uri = URI.parse(config.getUrl());
        String namespace = StringUtils.isEmpty(nacos.getNamespace()) ? NacosClientApi.DEFAULT_NAMESPACE : nacos.getNamespace();
        String username = StringUtils.isEmpty(nacos.getUsername()) ? "" : nacos.getUsername();
        String password = StringUtils.isEmpty(nacos.getPassword()) ? "" : nacos.getPassword();
        String name = username + ":" + password + "@" + uri.getAddress() + "/" + namespace;
        SharedNacosClientApi client = clients.computeIfAbsent(name, n -> new SharedNacosClientApi(new NacosClient(config)));
        client.reference.incrementAndGet();
        return client;
    }

    /**
     * An inner class that implements the NacosClientApi interface and provides a shared instance of NacosClient.
     */
    protected static class SharedNacosClientApi implements NacosClientApi {

        private final NacosClientApi api;

        private final AtomicInteger reference = new AtomicInteger(0);

        private final AtomicBoolean connected = new AtomicBoolean(false);

        public SharedNacosClientApi(NacosClientApi api) {
            this.api = api;
        }

        @Override
        public void connect() throws NacosException {
            if (!connected.get()) {
                synchronized (api) {
                    if (connected.compareAndSet(false, true)) {
                        api.connect();
                    }
                }
            }
        }

        @Override
        public void close() throws NacosException {
            if (reference.decrementAndGet() == 0 && connected.compareAndSet(true, false)) {
                api.close();
            }
        }

        @Override
        public void subscribe(String dataId, String group, Listener listener) throws NacosException {
            api.subscribe(dataId, group, listener);
        }

        @Override
        public void unsubscribe(String dataId, String group, Listener listener) {
            api.unsubscribe(dataId, group, listener);
        }

        @Override
        public <K extends NacosSyncKey, T> Syncer<K, T> createSyncer(Function<String, SyncResponse<T>> parser) {
            return api.createSyncer(parser);
        }
    }
}
