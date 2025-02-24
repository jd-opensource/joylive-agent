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
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.annotation.ConditionalOnConfigCenterEnabled;
import com.jd.live.agent.governance.config.ConfigCenterConfig;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.service.ConfigService;
import com.jd.live.agent.governance.subscription.config.ConfigName;
import com.jd.live.agent.governance.subscription.config.Configurator;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientApi;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientFactory;
import com.jd.live.agent.implement.service.config.nacos.client.NacosProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.implement.service.config.nacos.client.NacosClientApi.DEFAULT_NAMESPACE;

@Injectable
@ConditionalOnConfigCenterEnabled
@ConditionalOnProperty(name = "agent.governance.configcenter.type", value = "nacos")
@Extension("NacosConfigService")
public class NacosConfigService extends AbstractService implements ConfigService {

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    @Inject
    private Map<String, ConfigParser> parsers;

    private Configurator configurator;

    private final Map<String, NacosClientApi> clients = new ConcurrentHashMap<>();

    @Override
    public Configurator getConfigurator() {
        return configurator;
    }

    @Override
    public ConfigCenterConfig getConfig() {
        return governanceConfig.getConfigCenterConfig();
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        try {
            ConfigCenterConfig config = governanceConfig.getConfigCenterConfig();
            List<ConfigName> names = config.getConfigs();
            String namespace;
            NacosClientApi client = null;
            List<NacosSubscription> subscriptions = new ArrayList<>(names.size());
            for (ConfigName name : names) {
                if (name.validate()) {
                    namespace = name.getNamespace();
                    namespace = namespace == null || namespace.isEmpty() ? DEFAULT_NAMESPACE : namespace;
                    if (!clients.containsKey(namespace)) {
                        NacosProperties properties = new NacosProperties(config.getAddress(), config.getUsername(),
                                config.getPassword(), namespace, config.getTimeout());
                        client = NacosClientFactory.create(properties);
                        client.connect();
                        clients.put(namespace, client);
                    }
                    subscriptions.add(new NacosSubscription(client, name, getParser(name)));
                }
            }
            configurator = new NacosConfigurator(subscriptions);
            configurator.subscribe();
            return CompletableFuture.completedFuture(null);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        clients.forEach((namespace, client) -> Close.instance().close(client));
        clients.clear();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the appropriate ConfigParser for the given configuration name.
     *
     * @param configName The name of the configuration.
     * @return The ConfigParser associated with the given configuration name.
     */
    protected ConfigParser getParser(ConfigName configName) {
        String name = configName.getName();
        ConfigParser parser = parsers.get(ConfigParser.PROPERTIES);
        int index = name.lastIndexOf(".");
        if (index == -1) {
            return parser;
        }
        String type = name.substring(index + 1);
        return parsers.getOrDefault(type, parser);
    }
}
