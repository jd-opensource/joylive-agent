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
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.RegistrySupervisor;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.registry.springcloud.v2_2.instance.SpringEndpoint;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier.FixedServiceInstanceListSupplier;

import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * FixedInstanceSupplierConstructorInterceptor
 */
public class FixedInstanceSupplierConstructorInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(FixedInstanceSupplierConstructorInterceptor.class);

    private final RegistrySupervisor registry;

    public FixedInstanceSupplierConstructorInterceptor(RegistrySupervisor registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        FixedServiceInstanceListSupplier supplier = (FixedServiceInstanceListSupplier) ctx.getTarget();
        String service = ctx.getArgument(0);
        List<ServiceInstance> instances = ctx.getArgument(1);
        // subscribe policy
        registry.subscribe(service);
        logger.info("Found fixed instance supplier consumer, service: {}", service);
        UnsafeFieldAccessor accessor = getQuietly(FixedServiceInstanceListSupplier.class, "instances");
        if (accessor != null) {
            // subscribe instances
            registry.subscribe(service, event -> {
                // update merged instances
                List<ServiceInstance> newInstances = toList(event.getInstances(), i -> {
                    DefaultServiceInstance instance = new DefaultServiceInstance();
                    instance.setServiceId(service);
                    instance.setHost(i.getHost());
                    instance.setPort(i.getPort());
                    if (i.getMetadata() != null) {
                        instance.getMetadata().putAll(i.getMetadata());
                    }
                    return instance;
                });
                accessor.set(supplier, newInstances);
            });
            // update framework instances
            List<ServiceEndpoint> endpoints = toList(instances, SpringEndpoint::new);
            registry.update(service, endpoints);
        }
    }
}
