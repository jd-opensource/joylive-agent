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
import com.jd.live.agent.governance.policy.PolicySupplier;

import java.util.Map;

import static com.jd.live.agent.governance.util.Predicates.isDubboSystemService;

/**
 * AbstractConfigInterceptor
 */
public abstract class AbstractConfigInterceptor<T extends AbstractInterfaceConfig> extends InterceptorAdaptor {

    protected final Application application;

    protected final PolicySupplier policySupplier;

    public AbstractConfigInterceptor(Application application, PolicySupplier policySupplier) {
        this.application = application;
        this.policySupplier = policySupplier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        Map<String, String> map = getContext(ctx);
        T config = (T) ctx.getTarget();
        String service = getService(config);
        if (!isDubboSystemService(service)) {
            application.labelRegistry(map::putIfAbsent);
            policySupplier.subscribe(service);
        }

    }

    protected abstract Map<String, String> getContext(ExecutableContext ctx);

    protected abstract String getService(T config);

}
