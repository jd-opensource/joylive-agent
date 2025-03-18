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
package com.jd.live.agent.plugin.registry.springcloud.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * SimpleDiscoveryClientGetInstancesInterceptor
 */
public class SimpleDiscoveryClientGetInstancesInterceptor extends InterceptorAdaptor {

    private final Registry registry;

    public SimpleDiscoveryClientGetInstancesInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        String serviceId = ctx.getArgument(0);
        MethodContext mc = (MethodContext) ctx;
        List<ServiceEndpoint> endpoints = registry.getEndpoints(serviceId);
        if (endpoints != null && !endpoints.isEmpty()) {
            List<ServiceInstance> instances = toList(endpoints, e ->
                    new DefaultServiceInstance(
                            e.getId(),
                            e.getService(),
                            e.getHost(),
                            e.getPort(),
                            e.isSecure(),
                            e.getMetadata()));
            mc.skipWithResult(instances);
        }
    }
}
