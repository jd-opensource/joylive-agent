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

import com.jd.live.agent.governance.config.ConfigCenterConfig;
import com.jd.live.agent.governance.subscription.config.ConfigName;
import lombok.Getter;

import java.util.Map;

@Getter
public class ApolloProperties {

    public static final String KEY_ENV = "env";
    public static final String KEY_LABEL = "label";

    private final String address;

    private final String username;

    private final String password;

    private final ConfigName name;

    private final long timeout;

    private final Map<String, String> properties;

    public ApolloProperties(String address, String username, String password, ConfigName name, long timeout, Map<String, String> properties) {
        this.address = address;
        this.username = username;
        this.password = password;
        this.name = name;
        this.timeout = timeout;
        this.properties = properties;
    }

    public ApolloProperties(ConfigCenterConfig config, ConfigName name) {
        this(config.getAddress(), config.getUsername(), config.getPassword(), name, config.getTimeout(), config.getProperties());
    }

    public String getProperty(String key) {
        return properties == null || key == null ? null : properties.get(key);
    }

    public String getLabel() {
        return getProperty(KEY_LABEL);
    }

    public ApolloNameFormat getFormat() {
        return new ApolloNameFormat(name.getName());
    }

    public boolean validate() {
        return address != null && !address.isEmpty();
    }

    private String getNamespace() {
        return name.getNamespace();
    }

    private String getProfile() {
        return name.getProfile();
    }

}
