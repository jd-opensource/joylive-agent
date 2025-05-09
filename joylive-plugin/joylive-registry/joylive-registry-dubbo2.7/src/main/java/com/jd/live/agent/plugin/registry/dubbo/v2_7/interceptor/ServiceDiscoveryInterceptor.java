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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.registry.ServiceProtocol;

import java.util.Collections;
import java.util.Map;

/**
 * RegistryInterceptor
 */
public class ServiceDiscoveryInterceptor extends AbstractRegistryInterceptor {

    public ServiceDiscoveryInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected ServiceInstance getInstance(MethodContext ctx) {
        // TODO may called multiple times
        // multiple service discovery instances.
        // MultipleRegistryServiceDiscovery
        org.apache.dubbo.registry.client.ServiceInstance instance = ctx.getArgument(0);
        Map<String, String> metadata = instance.getMetadata();
        application.labelRegistry(metadata::putIfAbsent);
        return ServiceInstance.builder()
                .type("dubbo.v2_7")
                .service(instance.getServiceName())
                .host(instance.getHost())
                .port(instance.getPort())
                .protocols(Collections.singletonList(
                        ServiceProtocol.builder()
                                .host(instance.getHost())
                                .port(instance.getPort())
                                .metadata(metadata)
                                .build()))
                .build();
    }
}
