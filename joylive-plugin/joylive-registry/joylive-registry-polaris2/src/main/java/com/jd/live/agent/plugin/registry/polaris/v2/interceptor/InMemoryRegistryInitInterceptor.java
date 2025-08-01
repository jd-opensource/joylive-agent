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
package com.jd.live.agent.plugin.registry.polaris.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.plugin.registry.polaris.v2.registry.PolarisEventListener;
import com.jd.live.agent.plugin.registry.polaris.v2.registry.PolarisExecutorService;
import com.jd.live.agent.plugin.registry.polaris.v2.registry.PolarisRegistryService;
import com.tencent.polaris.plugins.registry.memory.InMemoryRegistry;

import java.util.concurrent.ExecutorService;

/**
 * InMemoryRegistryInitInterceptor
 */
public class InMemoryRegistryInitInterceptor extends AbstractInMemoryRegistryInterceptor {

    private final CompositeRegistry registry;

    public InMemoryRegistryInitInterceptor(CompositeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        InMemoryRegistry imRegistry = (InMemoryRegistry) ctx.getTarget();
        PolarisRegistryService registryService = new PolarisRegistryService(imRegistry);
        imRegistry.registerResourceListener(new PolarisEventListener(registryService));
        ExecutorService persistExecutor = (ExecutorService) Accessor.persistExecutor.get(imRegistry);
        Accessor.persistExecutor.set(imRegistry, new PolarisExecutorService(persistExecutor, registryService));
        registry.addSystemRegistry(registryService);
    }

}
