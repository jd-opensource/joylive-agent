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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ReferenceConfig;

import java.util.Map;

/**
 * ReferenceConfigInterceptor
 */
public class ReferenceConfigInterceptor extends InterceptorAdaptor {

    private final Application application;

    private final PolicySupplier policySupplier;

    public ReferenceConfigInterceptor(Application application, PolicySupplier policySupplier) {
        this.application = application;
        this.policySupplier = policySupplier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        AbstractInterfaceConfig config = (AbstractInterfaceConfig) ctx.getTarget();
        Map<String, String> map = (Map<String, String>) mc.getResult();

        application.label(map::putIfAbsent);
        String service = ((ReferenceConfig<?>) config).getProvidedBy();
        service = service != null && !service.isEmpty() ? service : config.getInterface();
        policySupplier.subscribe(service);
    }
}
