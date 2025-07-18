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
import com.jd.live.agent.plugin.registry.eureka.registry.EurekaRegistryConfig;
import com.jd.live.agent.plugin.registry.eureka.registry.EurekaRegistryService;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;

/**
 * DiscoveryClientConstructorInterceptor
 */
public class DiscoveryClientConstructorInterceptor extends InterceptorAdaptor {

    private final CompositeRegistry registry;

    public DiscoveryClientConstructorInterceptor(CompositeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        ApplicationInfoManager applicationInfoManager = (ApplicationInfoManager) arguments[0];
        EurekaClientConfig config = (EurekaClientConfig) arguments[1];
        String zone = applicationInfoManager.getInfo().getMetadata().get("zone");
        EurekaRegistryService registryService = new EurekaRegistryService(config, zone);
        arguments[1] = new EurekaRegistryConfig(config, registryService);
        registry.addSystemRegistry(registryService);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        DiscoveryClient client = (DiscoveryClient) ctx.getTarget();
        EurekaRegistryConfig config = (EurekaRegistryConfig) client.getEurekaClientConfig();
        EurekaRegistryService registry = (EurekaRegistryService) config.getPublisher();
        registry.setClient(client);
    }
}
