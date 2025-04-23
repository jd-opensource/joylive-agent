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
package com.jd.live.agent.plugin.registry.nacos.interceptor;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.client.naming.event.InstancesChangeEvent;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.RegistryService;
import com.jd.live.agent.plugin.registry.nacos.registry.NacosRegistryService;

import static com.jd.live.agent.plugin.registry.nacos.registry.NacosRegistryService.getEndpoints;

/**
 * NacosInstanceChangeInterceptor
 */
public class NacosInstanceChangeInterceptor extends InterceptorAdaptor {

    private final CompositeRegistry registry;

    public NacosInstanceChangeInterceptor(CompositeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        RegistryService service = registry.getSystemRegistry();
        if (service instanceof NacosRegistryService) {
            InstancesChangeEvent event = ctx.getArgument(0);
            NacosRegistryService nacos = (NacosRegistryService) service;
            nacos.publish(new RegistryEvent(event.getServiceName(),
                    event.getGroupName(),
                    getEndpoints(event.getHosts()),
                    Constants.DEFAULT_GROUP));
        }
    }
}
