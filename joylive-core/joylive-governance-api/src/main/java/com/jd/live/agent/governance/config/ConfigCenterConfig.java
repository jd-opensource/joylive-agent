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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.splitList;

@Getter
@Setter
public class ConfigCenterConfig {

    private boolean enabled;

    private String type;

    private String address;

    private String username;

    private String password;

    private List<String> names;

    private ConfigName name = new ConfigName();

    private RefreshConfig refresh = new RefreshConfig();

    private long timeout = 3000;

    private boolean grayEnabled = true;

    private Map<String, String> properties;

    private transient List<ConfigName> configs;

    public boolean isEnabled(String beanName, Object bean) {
        return refresh == null || refresh.isEnabled(beanName, bean);
    }

    public String getProperty(String key) {
        return key == null || properties == null ? null : properties.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return key == null || properties == null ? defaultValue : properties.getOrDefault(key, defaultValue);
    }

    public List<ConfigName> getConfigs() {
        if (configs == null) {
            configs = new ArrayList<>(4);
            if (names != null && !names.isEmpty()) {
                List<String> values = new ArrayList<>(3);
                for (String name : names) {
                    values.clear();
                    // name@profile@namespace
                    splitList(name, c -> c == '@', true, true, null, values::add);
                    if (values.size() >= 3) {
                        configs.add(new ConfigName(values.get(2), values.get(0), values.get(1)));
                    } else if (values.size() == 2) {
                        configs.add(new ConfigName(null, values.get(0), values.get(1)));
                    } else if (values.size() == 1) {
                        configs.add(new ConfigName(null, values.get(0), null));
                    }
                }
            }
            if (configs.isEmpty() && name != null) {
                configs.add(name);
            }
        }
        return configs;
    }
}

