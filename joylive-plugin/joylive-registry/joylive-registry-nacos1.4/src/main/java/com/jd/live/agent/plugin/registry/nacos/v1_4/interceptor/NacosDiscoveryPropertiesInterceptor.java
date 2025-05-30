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
package com.jd.live.agent.plugin.registry.nacos.v1_4.interceptor;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

import java.util.Properties;

/**
 * NacosDiscoveryPropertiesInterceptor
 */
public class NacosDiscoveryPropertiesInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        NacosDiscoveryProperties discoveryProperties = (NacosDiscoveryProperties) mc.getTarget();
        // set group, namespace and secure
        Properties properties = mc.getResult();
        properties.setProperty(Constants.LABEL_GROUP, discoveryProperties.getGroup());
        properties.setProperty(Constants.LABEL_NAMESPACE, discoveryProperties.getNamespace());
        if (discoveryProperties.isSecure()) {
            properties.put(Constants.LABEL_SECURE, "true");
        }
    }
}
