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
package com.jd.live.agent.plugin.registry.springcloud.v2_2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.plugin.registry.springcloud.v2_2.instance.SpringEndpoint;
import com.jd.live.agent.plugin.registry.springcloud.v2_2.registry.FixedRegistryService;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * FixedInstanceSupplierInterceptor
 */
public class FixedInstanceSupplierInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(FixedInstanceSupplierInterceptor.class);

    private final CompositeRegistry registry;

    public FixedInstanceSupplierInterceptor(CompositeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        String service = ctx.getArgument(0);
        List<ServiceInstance> instances = ctx.getArgument(1);
        if (instances != null && !instances.isEmpty()) {
            // subscribe policy
            registry.subscribe(service);
            logger.info("Found fixed instance supplier consumer, service: {}", service);
            registry.addSystemRegistry(service, new FixedRegistryService(service, toList(instances, SpringEndpoint::new)));
        }
    }
}
