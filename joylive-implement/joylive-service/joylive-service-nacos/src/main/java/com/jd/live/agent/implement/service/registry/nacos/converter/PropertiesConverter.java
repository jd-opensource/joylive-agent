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
package com.jd.live.agent.implement.service.registry.nacos.converter;

import com.jd.live.agent.core.util.converter.Converter;
import com.jd.live.agent.governance.config.RegistryClusterConfig;

import java.util.Properties;

import static com.alibaba.nacos.api.PropertyKeyConst.*;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;

public class PropertiesConverter implements Converter<RegistryClusterConfig, Properties> {

    public static final Converter<RegistryClusterConfig, Properties> INSTANCE = new PropertiesConverter();

    @Override
    public Properties convert(RegistryClusterConfig source) {
        Properties result = new Properties();
        if (source.getProperties() != null) {
            result.putAll(source.getProperties());
        }
        if (!isEmpty(source.getNamespace())) {
            result.put(NAMESPACE, source.getNamespace());
        }
        if (!isEmpty(source.getUsername())) {
            result.put(USERNAME, source.getUsername());
            result.put(PASSWORD, source.getPassword());
        }
        if (source.isDenyEmptyEnabled()) {
            result.put(NAMING_PUSH_EMPTY_PROTECTION, "true");
        }
        return result;
    }
}
