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
package com.jd.live.agent.implement.service.config.apollo.client;

import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.jd.live.agent.core.exception.ConfigException;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.service.config.AbstractSharedClientApi;
import com.jd.live.agent.governance.subscription.config.ConfigName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

/**
 * A factory class for creating and managing shared instances of ApolloClientApi.
 * It ensures that clients are reused when configurations are identical.
 */
public abstract class ApolloClientFactory {

    private static final Map<String, ApolloSharedClientApi> clients = new ConcurrentHashMap<>();

    /**
     * Creates or retrieves a shared ApolloClientApi instance based on the provided Apollo properties.
     *
     * @param properties The properties containing configuration details for the Apollo client.
     * @return A shared ApolloClientApi instance.
     */
    public static ApolloClientApi create(ApolloProperties properties) {
        if (!properties.validate()) {
            throw new ConfigException("Invalid config center address: " + properties.getAddress());
        }
        URI uri = URI.parse(properties.getAddress());
        ConfigName configName = properties.getName();
        String name = isEmpty(configName.getName()) ? "" : configName.getName();
        String username = isEmpty(properties.getUsername()) ? "" : properties.getUsername();
        String password = isEmpty(properties.getPassword()) ? "" : properties.getPassword();
        String key = username + ":" + password + "@" + uri.getAddress() + "/" + name;
        ApolloSharedClientApi api = clients.computeIfAbsent(key, n -> new ApolloSharedClientApi(n, new ApolloClient(properties), clients::remove));
        api.incReference();
        return api;
    }

    /**
     * An inner class that implements the NacosClientApi interface and provides a shared instance of NacosClient.
     */
    private static class ApolloSharedClientApi extends AbstractSharedClientApi<ApolloClientApi> implements ApolloClientApi {

        ApolloSharedClientApi(String name, ApolloClientApi api, Consumer<String> cleaner) {
            super(name, api, cleaner);
        }

        @Override
        public void subscribe(ConfigFileChangeListener listener) {
            api.subscribe(listener);
        }

        @Override
        public void unsubscribe(ConfigFileChangeListener listener) {
            api.unsubscribe(listener);
        }
    }

}
