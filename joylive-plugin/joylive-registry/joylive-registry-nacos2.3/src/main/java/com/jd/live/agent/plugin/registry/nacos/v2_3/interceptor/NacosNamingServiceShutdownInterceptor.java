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
package com.jd.live.agent.plugin.registry.nacos.v2_3.interceptor;

import com.alibaba.nacos.client.naming.NacosNamingService;
import com.alibaba.nacos.client.naming.event.InstancesChangeNotifier;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.plugin.registry.nacos.v2_3.registry.NacosInstancesChangeNotifier;
import com.jd.live.agent.plugin.registry.nacos.v2_3.registry.NacosRegistryService;

/**
 * NacosNamingServiceConstructorInterceptor
 */
public class NacosNamingServiceShutdownInterceptor extends InterceptorAdaptor {

    private final CompositeRegistry supervisor;

    public NacosNamingServiceShutdownInterceptor(CompositeRegistry supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        NacosNamingService client = (NacosNamingService) ctx.getTarget();
        InstancesChangeNotifier changeNotifier = FieldAccessorFactory.getQuietly(client, "changeNotifier");
        if (changeNotifier instanceof NacosInstancesChangeNotifier) {
            NacosInstancesChangeNotifier notifier = (NacosInstancesChangeNotifier) changeNotifier;
            NacosRegistryService service = (NacosRegistryService) notifier.getPublisher();
            supervisor.removeSystemRegistry(service);
        }
    }
}
