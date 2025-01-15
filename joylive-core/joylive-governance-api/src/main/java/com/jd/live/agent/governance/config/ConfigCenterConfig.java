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
package com.jd.live.agent.governance.config;

import com.jd.live.agent.governance.subscription.config.ConfigName;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ConfigCenterConfig {

    private String type;

    private String address;

    private String username;

    private String password;

    private ConfigName name = new ConfigName();

    private RefreshConfig refresh = new RefreshConfig();

    private long timeout = 3000;

    private Map<String, String> properties;

    public boolean isEnabled(String beanName, Object bean) {
        return refresh == null || refresh.isEnabled(beanName, bean);
    }

    public String getProperty(String key) {
        return key == null || properties == null ? null : properties.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return key == null || properties == null ? defaultValue : properties.getOrDefault(key, defaultValue);
    }

}

