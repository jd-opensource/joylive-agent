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
package com.jd.live.agent.plugin.registry.dubbo.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.bootstrap.AgentLifecycle;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.registry.ServiceProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RegistryInterceptor
 */
public class RegistryInterceptor extends AbstractRegistryInterceptor {

    public RegistryInterceptor(Application application, AgentLifecycle lifecycle, Registry registry) {
        super(application, lifecycle, registry);
    }

    @Override
    protected ServiceInstance getInstance(MethodContext ctx) {
        org.apache.dubbo.registry.client.ServiceInstance instance = ctx.getArgument(0);
        Map<String, String> metadata = instance.getMetadata();
        application.labelRegistry(metadata::putIfAbsent);
        List<ServiceProtocol> protocols = new ArrayList<>();
        instance.getServiceMetadata().getServices().forEach((name, info) -> {
            protocols.add(ServiceProtocol.builder()
                    .group(info.getGroup())
                    .path(info.getPath())
                    .schema(info.getProtocol())
                    .host(instance.getHost())
                    .port(info.getPort())
                    .metadata(info.getParams())
                    .build());
        });
        return ServiceInstance.builder()
                .type("dubbo.v3")
                .service(instance.getServiceName())
                .host(instance.getHost())
                .port(instance.getPort())
                .metadata(metadata)
                .protocols(protocols)
                .build();
    }
}

