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
package com.jd.live.agent.plugin.registry.springcloud.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.registry.springcloud.v5.instance.SpringEndpoint;
import com.jd.live.agent.plugin.registry.springcloud.v5.registry.SimpleRegistryService;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * SimpleDiscoveryClientConstructorInterceptor
 */
public class SimpleDiscoveryClientInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDiscoveryClientInterceptor.class);

    private final CompositeRegistry registry;

    public SimpleDiscoveryClientInterceptor(CompositeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        SimpleDiscoveryProperties properties = ctx.getArgument(0);
        Map<String, List<DefaultServiceInstance>> simples = properties.getInstances();
        Map<String, List<ServiceEndpoint>> instances = new HashMap<>(simples.size());
        simples.forEach((service, instance) -> {
            registry.register(service);
            logger.info("Found simple discovery client provider, service: {}", service);
            instances.put(service, toList(instance, SpringEndpoint::new));
        });
        registry.addSystemRegistry(new SimpleRegistryService(instances));
    }
}
