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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

/**
 * EurekaServiceRegistryInterceptor
 */
public class EurekaServiceRegistryInterceptor extends InterceptorAdaptor {

    @Override
    public void onEnter(ExecutableContext ctx) {
        EurekaRegistration registration = ctx.getArgument(0);
        // info is used to register
        InstanceInfo info = registration.getApplicationInfoManager().getInfo();
        if (registration.getMetadata() != null
                && info.getMetadata() != null
                && info.getMetadata() != registration.getMetadata()) {
            info.getMetadata().putAll(registration.getMetadata());
        }
    }
}
