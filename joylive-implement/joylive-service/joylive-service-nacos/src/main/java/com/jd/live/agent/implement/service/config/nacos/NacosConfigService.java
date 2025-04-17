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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.governance.annotation.ConditionalOnConfigCenterEnabled;
import com.jd.live.agent.governance.service.config.AbstractConfigService;
import com.jd.live.agent.governance.service.config.ConfigSubscription;
import com.jd.live.agent.governance.subscription.config.ConfigName;
import com.jd.live.agent.governance.subscription.config.Configurator;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientApi;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientFactory;
import com.jd.live.agent.implement.service.config.nacos.client.NacosProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;

@Injectable
@ConditionalOnConfigCenterEnabled
@ConditionalOnProperty(name = "agent.governance.configCenter.type", value = "nacos")
@Extension("NacosConfigService")
public class NacosConfigService extends AbstractConfigService<NacosClientApi> {

    @Inject(ObjectParser.JSON)
    private ObjectParser json;

    protected final Map<String, NacosClientApi> clients = new ConcurrentHashMap<>();

    @Override
    protected ConfigSubscription<NacosClientApi> createSubscription(ConfigName configName) throws Exception {
        String namespace = configName.getNamespace();
        namespace = namespace == null || namespace.isEmpty() ? DEFAULT_NAMESPACE_ID : namespace;
        NacosClientApi client = clients.computeIfAbsent(namespace, n -> NacosClientFactory.create(getProperties(n), json));
        client.connect();
        return new ConfigSubscription<>(client, configName, getParser(configName));
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        clients.forEach((namespace, client) -> Close.instance().close(client));
        clients.clear();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected Configurator createConfigurator(List<ConfigSubscription<NacosClientApi>> subscriptions) {
        return new NacosConfigurator(subscriptions);
    }

    /**
     * Creates and returns a {@code NacosProperties} instance initialized with configuration center settings
     * and the specified namespace.
     *
     * @param namespace the namespace to be used for Nacos configuration isolation (must not be {@code null})
     * @return a new {@code NacosProperties} instance configured with the specified namespace
     */
    private NacosProperties getProperties(String namespace) {
        return new NacosProperties(governanceConfig.getConfigCenterConfig(), namespace);
    }
}
