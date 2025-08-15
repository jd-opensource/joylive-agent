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
package com.jd.live.agent.plugin.registry.eureka.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.CloudEurekaInstanceConfig;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

import java.util.Map;

/**
 * EurekaServiceRegistryInterceptor
 */
public class EurekaServiceRegistryInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        EurekaRegistration registration = ctx.getArgument(0);
        Map<String, String> metadata = null;
        if (registration instanceof ServiceInstance) {
            metadata = registration.getMetadata();
        } else {
            // fix for eureka 1.x
            CloudEurekaInstanceConfig config = (CloudEurekaInstanceConfig) Accessor.instanceConfig.get(registration);
            if (config != null) {
                metadata = config.getMetadataMap();
            }
        }
        // info is used to register
        InstanceInfo info = registration.getApplicationInfoManager().getInfo();
        if (metadata != null
                && info.getMetadata() != null
                && info.getMetadata() != metadata) {
            info.getMetadata().putAll(metadata);
        }
    }

    private static class Accessor {

        private static final FieldAccessor instanceConfig = FieldAccessorFactory.getAccessor(EurekaRegistration.class, "instanceConfig");

    }
}
