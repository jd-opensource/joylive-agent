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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.interceptor;

import com.alibaba.dubbo.config.AbstractInterfaceConfig;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;

import java.util.Map;

import static com.jd.live.agent.governance.util.Predicates.isDubboSystemService;

/**
 * AbstractConfigInterceptor
 */
public abstract class AbstractConfigInterceptor<T extends AbstractInterfaceConfig> extends InterceptorAdaptor {

    protected final Application application;

    protected final Registry registry;

    public AbstractConfigInterceptor(Application application, Registry registry) {
        this.application = application;
        this.registry = registry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        Map<String, String> map = getContext(ctx);
        T config = (T) ctx.getTarget();
        String service = getService(config);
        if (!isDubboSystemService(service)) {
            application.labelRegistry(map::putIfAbsent);
            subscribe(service);
        }
    }

    protected abstract Map<String, String> getContext(ExecutableContext ctx);

    protected abstract String getService(T config);

    protected void subscribe(String service) {
        registry.subscribe(service);
    }

}
