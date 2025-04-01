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

import com.alibaba.nacos.api.config.listener.Listener;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.service.config.AbstractConfigurator;
import com.jd.live.agent.governance.service.config.ConfigSubscription;
import com.jd.live.agent.governance.subscription.config.ConfigName;
import com.jd.live.agent.governance.subscription.config.Configurator;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientApi;

import java.util.List;
import java.util.concurrent.Executor;

import static com.jd.live.agent.implement.service.config.nacos.client.NacosClientApi.DEFAULT_GROUP;

/**
 * A configurator that uses Nacos as the configuration source.
 * <p>
 * This class implements the {@link Configurator} interface and provides methods for subscribing to configuration changes,
 * getting properties, adding and removing listeners, and publishing configuration events.
 */
public class NacosConfigurator extends AbstractConfigurator<NacosClientApi> {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfigurator.class);

    public NacosConfigurator(List<ConfigSubscription<NacosClientApi>> subscriptions) {
        super(subscriptions);
    }

    @Override
    public String getName() {
        return "nacos";
    }

    @Override
    protected void subscribe(ConfigSubscription<NacosClientApi> subscription) throws Exception {
        ConfigName name = subscription.getName();
        String profile = name.getProfile();
        profile = profile == null || profile.isEmpty() ? DEFAULT_GROUP : profile;
        logger.info("Subscribe {}, parser {}", name, subscription.getParser().getClass().getSimpleName());
        subscription.getClient().subscribe(name.getName(), profile, new ChangeListener(subscription));
    }

    /**
     * An inner class that implements the Listener interface and handles configuration changes.
     */
    private class ChangeListener implements Listener {

        private final ConfigSubscription<NacosClientApi> subscription;

        ChangeListener(ConfigSubscription<NacosClientApi> subscription) {
            this.subscription = subscription;
        }

        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public void receiveConfigInfo(String configInfo) {
            onUpdate(configInfo, subscription);
        }
    }
}
