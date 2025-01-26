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

import com.alibaba.nacos.client.naming.event.InstancesChangeEvent;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.RegistrySupervisor;
import com.jd.live.agent.plugin.registry.nacos.instance.NacosEndpoint;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * NacosInstanceChangeInterceptor
 */
public class NacosInstanceChangeInterceptor extends InterceptorAdaptor {

    private final RegistrySupervisor supervisor;

    public NacosInstanceChangeInterceptor(RegistrySupervisor supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        InstancesChangeEvent event = mc.getArgument(0);
        List<NacosEndpoint> endpoints = toList(event.getHosts(), NacosEndpoint::new);
        // get group from meta
        supervisor.update(event.getServiceName(), endpoints);

    }
}
