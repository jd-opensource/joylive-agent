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

import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.core.exception.ConfigException;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.service.config.AbstractSharedClientApi;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.nacos.NacosSyncKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;

public abstract class NacosClientFactory {

    private static final Map<String, SharedNacosClientApi> clients = new ConcurrentHashMap<>();

    /**
     * Creates a new instance of NacosClientApi using the provided configuration.
     *
     * @param config The NacosProperties object containing the configuration for the client.
     * @return A new instance of NacosClientApi.
     */
    public static NacosClientApi create(NacosProperties config) {
        return create(config, null, null);
    }

    /**
     * Creates a new instance of NacosClientApi using the provided configuration.
     *
     * @param config      The NacosProperties object containing the configuration for the client.
     * @param json        The json parser used to parse the policy.
     * @param application The application instance.
     * @return A new instance of NacosClientApi.
     */
    public static NacosClientApi create(NacosProperties config, ObjectParser json, Application application) {
        URI uri = URI.parse(config.getUrl());
        if (uri == null) {
            throw new ConfigException("Invalid config center address: " + config.getUrl());
        }
        String namespace = StringUtils.isEmpty(config.getNamespace()) ? DEFAULT_NAMESPACE_ID : config.getNamespace();
        String username = StringUtils.isEmpty(config.getUsername()) ? "" : config.getUsername();
        String password = StringUtils.isEmpty(config.getPassword()) ? "" : config.getPassword();
        String name = username + ":" + password + "@" + uri.getAddress() + "/" + namespace;
        SharedNacosClientApi client = clients.computeIfAbsent(name, n ->
                new SharedNacosClientApi(n, new NacosClient(config, json, application), clients::remove));
        client.incReference();
        return client;
    }

    /**
     * An inner class that implements the NacosClientApi interface and provides a shared instance of NacosClient.
     */
    private static class SharedNacosClientApi extends AbstractSharedClientApi<NacosClientApi> implements NacosClientApi {

        SharedNacosClientApi(String name, NacosClientApi api, Consumer<String> cleaner) {
            super(name, api, cleaner);
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
        public <K extends NacosSyncKey, T> Syncer<K, T> createSyncer(BiFunction<K, String, SyncResponse<T>> parser) {
            return api.createSyncer(parser);
        }
    }
}
