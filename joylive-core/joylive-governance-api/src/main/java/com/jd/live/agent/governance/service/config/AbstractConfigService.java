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
package com.jd.live.agent.governance.service.config;

import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.config.ConfigCenterConfig;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.service.ConfigService;
import com.jd.live.agent.governance.subscription.config.ConfigName;
import com.jd.live.agent.governance.subscription.config.Configurator;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for configuration services.
 *
 * @param <T> The type of {@link ConfigClientApi} used for configuration.
 */
public abstract class AbstractConfigService<T extends ConfigClientApi> extends AbstractService implements ConfigService, InjectSourceSupplier {

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    protected GovernanceConfig governanceConfig;

    @Inject
    protected Map<String, ConfigParser> parsers;

    protected Configurator configurator;

    @Override
    public Configurator getConfigurator() {
        return configurator;
    }

    @Override
    public ConfigCenterConfig getConfig() {
        return governanceConfig.getConfigCenterConfig();
    }

    @Override
    public void apply(InjectSource source) {
        source.add(ConfigService.COMPONENT_CONFIG_CENTER, this);
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        try {
            // TODO Wait config ready
            ConfigCenterConfig config = governanceConfig.getConfigCenterConfig();
            Set<ConfigName> uniqueNames = new HashSet<>();
            List<ConfigName> names = config.getConfigs();
            List<ConfigSubscription<T>> subscriptions = new ArrayList<>(names.size());
            for (ConfigName name : names) {
                if (name.validate() && uniqueNames.add(name)) {
                    ConfigSubscription<T> subscription = createSubscription(name);
                    if (subscription != null) {
                        subscriptions.add(subscription);
                    }
                }
            }
            configurator = createConfigurator(subscriptions);
            configurator.subscribe();
            return CompletableFuture.completedFuture(null);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    /**
     * Returns the appropriate ConfigParser for the given configuration name.
     *
     * @param configName The name of the configuration.
     * @return The ConfigParser associated with the given configuration name.
     */
    protected ConfigParser getParser(ConfigName configName) {
        return parsers.getOrDefault(getFormat(configName), parsers.get(ConfigParser.PROPERTIES));
    }

    /**
     * Retrieves the format of the configuration.
     *
     * @param configName The name of the configuration.
     * @return The format of the configuration.
     */
    protected String getFormat(ConfigName configName) {
        return configName.getFormat();
    }

    /**
     * Creates a subscription for the given configuration name.
     *
     * @param configName The name of the configuration.
     * @return A {@link ConfigSubscription} instance.
     * @throws Exception If the subscription creation fails.
     */
    protected abstract ConfigSubscription<T> createSubscription(ConfigName configName) throws Exception;

    /**
     * Creates a configurator for the given list of subscriptions.
     *
     * @param subscriptions The list of subscriptions.
     * @return A {@link Configurator} instance.
     */
    protected abstract Configurator createConfigurator(List<ConfigSubscription<T>> subscriptions);
}
