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
package com.jd.live.agent.plugin.router.springcloud.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * SimpleReactiveDiscoveryClientGetInstancesInterceptor
 */
public class SimpleReactiveDiscoveryClientGetInstancesInterceptor extends InterceptorAdaptor {

    private final Registry registry;

    public SimpleReactiveDiscoveryClientGetInstancesInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        String serviceId = ctx.getArgument(0);
        MethodContext mc = (MethodContext) ctx;
        List<ServiceEndpoint> endpoints = registry.getEndpoints(serviceId);
        if (endpoints != null && !endpoints.isEmpty()) {
            List<ServiceInstance> instances = CollectionUtils.toList(endpoints, e -> {
                DefaultServiceInstance instance = new DefaultServiceInstance();
                instance.setServiceId(serviceId);
                instance.setHost(e.getHost());
                instance.setPort(e.getPort());
                if (e.getMetadata() != null) {
                    instance.getMetadata().putAll(e.getMetadata());
                }
                return instance;
            });
            mc.skipWithResult(Flux.fromIterable(instances));
        }
    }
}
