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
package com.jd.live.agent.plugin.registry.dubbo.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.registry.dubbo.v3.registry.DubboExecutorService;
import com.jd.live.agent.plugin.registry.dubbo.v3.registry.DubboRegistryService;
import org.apache.dubbo.registry.Registry;

import java.util.concurrent.ScheduledExecutorService;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;
import static com.jd.live.agent.plugin.registry.dubbo.v3.registry.DubboExecutorService.FIELD_REGISTRY_CACHE_EXECUTOR;

/**
 * FailbackRegistryConstructorInterceptor
 */
public class FailbackRegistryConstructorInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        Registry registry = (Registry) ctx.getTarget();
        ScheduledExecutorService executor = getQuietly(registry, FIELD_REGISTRY_CACHE_EXECUTOR);
        if (executor != null) {
            executor = new DubboExecutorService(executor, new DubboRegistryService(registry));
            setValue(registry, FIELD_REGISTRY_CACHE_EXECUTOR, executor);
        }
    }
}
