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

import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.jd.live.agent.governance.subscription.config.ConfigName;

import static com.jd.live.agent.implement.service.config.apollo.client.ApolloProperties.KEY_ENV;

/**
 * A client for interacting with the Nacos configuration service.
 */
public class ApolloClient implements ApolloClientApi {

    private final ApolloProperties properties;

    private ConfigFile client;

    public ApolloClient(ApolloProperties properties) {
        this.properties = properties;
    }

    @Override
    public void connect() throws Exception {
        // TODO use open api, HttpWatcher
        // @see com.ctrip.framework.apollo.internals.RemoteConfigRepository.transformApolloConfigToProperties
        ConfigName name = properties.getName();
        setProperty(ApolloClientSystemConsts.APOLLO_CACHE_FILE_ENABLE, "false");
        setProperty(KEY_ENV, name.getNamespace());
        setProperty(ConfigConsts.APOLLO_CLUSTER_KEY, name.getProfile());
        setProperty(ApolloClientSystemConsts.APOLLO_LABEL, properties.getLabel());
        setProperty(ApolloClientSystemConsts.APP_ID, properties.getUsername());
        setProperty(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET, properties.getPassword());
        ApolloAddress address = ApolloAddress.parse(properties.getAddress());
        setProperty(address.getType() == AddressType.META_SERVER
                        ? ConfigConsts.APOLLO_META_KEY
                        : ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE,
                address.getAddress());

        ApolloNameFormat format = properties.getFormat();
        client = ConfigService.getConfigFile(format.getName(), format.getFormat());
    }

    @Override
    public void close() {
    }

    @Override
    public void subscribe(ConfigFileChangeListener listener) {
        if (listener != null) {
            client.addChangeListener(listener);
            if (client.hasContent()) {
                // the client will not fire the change event.
                listener.onChange(new ConfigFileChangeEvent(properties.getName().getName(), null, client.getContent(), PropertyChangeType.ADDED));
            }
        }
    }

    @Override
    public void unsubscribe(ConfigFileChangeListener listener) {
        if (listener != null) {
            client.removeChangeListener(listener);
        }
    }

    private void setProperty(String key, String value) {
        if (value != null && !value.isEmpty()) {
            System.setProperty(key, value);
        }
    }

    private static class Address {

        private String type;

        private String url;

    }
}
