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
package com.jd.live.agent.governance.subscription.config;

import com.jd.live.agent.core.bootstrap.AppPropertySource;

import static com.jd.live.agent.core.util.StringUtils.getKebabToCamel;

/**
 * A property source implementation backed by a Configurator.
 */
public class ConfiguratorSource implements AppPropertySource {

    private final Configurator configurator;

    public ConfiguratorSource(Configurator configurator) {
        this.configurator = configurator;
    }

    @Override
    public String getProperty(String name) {
        if (name == null || configurator == null) {
            return null;
        }
        Object property = configurator.getProperty(name);
        if (property == null) {
            String key = getKebabToCamel(name);
            if (key != null) {
                property = configurator.getProperty(key);
            }
        }
        return property == null ? null : property.toString();
    }

    @Override
    public String getName() {
        return configurator.getName();
    }
}