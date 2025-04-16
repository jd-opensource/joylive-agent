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
package com.jd.live.agent.plugin.registry.eureka.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.RegistryService;
import com.jd.live.agent.plugin.registry.eureka.registry.EurekaRegistryService;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

/**
 * DeltaUpdateInterceptor
 */
public class DiscoveryClientDeltaUpdateInterceptor extends InterceptorAdaptor {

    private final CompositeRegistry registry;

    public DiscoveryClientDeltaUpdateInterceptor(CompositeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        RegistryService service = registry.getSystemRegistry();
        if (service instanceof EurekaRegistryService) {
            DiscoveryClient client = (DiscoveryClient) ctx.getTarget();
            Applications applications = ctx.getArgument(0);
            EurekaRegistryService eureka = (EurekaRegistryService) service;
            // delta update
            for (Application app : applications.getRegisteredApplications()) {
                // full instances
                eureka.publish(client.getApplication(app.getName()));
            }
        }
    }
}
