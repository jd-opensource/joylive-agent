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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.policy.PolicySupplier;
import org.apache.dubbo.config.ReferenceConfig;

import java.util.Map;

/**
 * ServiceConfigInterceptor
 */
public class ReferenceConfigInterceptor extends AbstractConfigInterceptor<ReferenceConfig<?>> {

    public ReferenceConfigInterceptor(Application application, PolicySupplier policySupplier) {
        super(application, policySupplier);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, String> getContext(ExecutableContext ctx) {
        return (Map<String, String>) ctx.getArguments()[0];
    }

    @Override
    protected int getRegistryType(ReferenceConfig<?> config) {
        String service = config.getProvidedBy();
        return service == null || service.isEmpty() ? REGISTRY_TYPE_INTERFACE : REGISTRY_TYPE_SERVICE;
    }
}
