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
package com.jd.live.agent.implement.service.config.nacos.client.converter;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.jd.live.agent.core.util.converter.Converter;
import com.jd.live.agent.implement.service.config.nacos.client.NacosProperties;

import java.util.Properties;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;

public class PropertiesConverter implements Converter<NacosProperties, Properties> {

    public static final Converter<NacosProperties, Properties> INSTANCE = new PropertiesConverter();

    @Override
    public Properties convert(NacosProperties source) {
        Properties result = new Properties();
        if (source.getProperties() != null) {
            result.putAll(source.getProperties());
        }
        if (!isEmpty(source.getNamespace()) && !DEFAULT_NAMESPACE_ID.equals(source.getNamespace())) {
            result.put(PropertyKeyConst.NAMESPACE, source.getNamespace());
        }
        if (!isEmpty(source.getUsername())) {
            result.put(PropertyKeyConst.USERNAME, source.getUsername());
            result.put(PropertyKeyConst.PASSWORD, source.getPassword());
        }
        return result;
    }
}
