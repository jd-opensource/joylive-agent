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

import com.alibaba.nacos.client.naming.NacosNamingService;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.plugin.registry.nacos.v1_4.registry.NacosInstancePublisher;
import com.jd.live.agent.plugin.registry.nacos.v1_4.registry.NacosRegistryService;

import java.util.Properties;

import static com.jd.live.agent.governance.registry.RegistryService.KEY_SYSTEM_REGISTERED;
import static com.jd.live.agent.governance.registry.RegistryService.SYSTEM_REGISTERED_PREDICATE;
import static com.jd.live.agent.plugin.registry.nacos.v1_4.registry.NacosRegistryPublisher.LOCAL_PUBLISHER;

/**
 * NacosNamingServiceConstructorInterceptor
 */
public class NacosNamingServiceInitInterceptor extends InterceptorAdaptor {

    private final CompositeRegistry supervisor;

    public NacosNamingServiceInitInterceptor(CompositeRegistry supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // HostReactor.constructor -> InstancesChangeNotifier.constructor -> NotifyCenter.registerSubscriber
        Properties properties = ctx.getArgument(0);
        // for dubbo
        if (!SYSTEM_REGISTERED_PREDICATE.test(properties.getProperty(KEY_SYSTEM_REGISTERED))) {
            NacosRegistryService registry = new NacosRegistryService(null, properties);
            LOCAL_PUBLISHER.set(new NacosInstancePublisher(registry, null));
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        NacosInstancePublisher publisher = LOCAL_PUBLISHER.get();
        if (publisher != null) {
            LOCAL_PUBLISHER.remove();
            // set client
            NacosNamingService target = (NacosNamingService) ctx.getTarget();
            NacosRegistryService registryService = (NacosRegistryService) publisher.getPublisher();
            registryService.setClient(target);
            supervisor.addSystemRegistry(registryService);
        }
    }
}
