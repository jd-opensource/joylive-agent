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
package com.jd.live.agent.implement.service.config.apollo;

import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.service.config.AbstractConfigurator;
import com.jd.live.agent.governance.service.config.ConfigSubscription;
import com.jd.live.agent.governance.subscription.config.ConfigName;
import com.jd.live.agent.governance.subscription.config.Configurator;
import com.jd.live.agent.implement.service.config.apollo.client.ApolloClientApi;

import java.util.List;

/**
 * A configurator that uses Nacos as the configuration source.
 * <p>
 * This class implements the {@link Configurator} interface and provides methods for subscribing to configuration changes,
 * getting properties, adding and removing listeners, and publishing configuration events.
 */
public class ApolloConfigurator extends AbstractConfigurator<ApolloClientApi> {

    private static final Logger logger = LoggerFactory.getLogger(ApolloConfigurator.class);

    public ApolloConfigurator(List<ConfigSubscription<ApolloClientApi>> subscriptions) {
        super(subscriptions);
    }

    @Override
    public String getName() {
        return "apollo";
    }

    @Override
    protected void subscribe(ConfigSubscription<ApolloClientApi> subscription) throws Exception {
        ConfigName name = subscription.getName();
        logger.info("subscribe " + name + ", parser " + subscription.getParser().getClass().getSimpleName());
        subscription.getClient().subscribe(new ChangeListener(subscription));
    }

    /**
     * An inner class that implements the Listener interface and handles configuration changes.
     */
    private class ChangeListener implements ConfigFileChangeListener {

        private final ConfigSubscription<ApolloClientApi> subscription;

        ChangeListener(ConfigSubscription<ApolloClientApi> subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onChange(ConfigFileChangeEvent changeEvent) {
            onUpdate(changeEvent.getNewValue(), subscription);
        }
    }
}
