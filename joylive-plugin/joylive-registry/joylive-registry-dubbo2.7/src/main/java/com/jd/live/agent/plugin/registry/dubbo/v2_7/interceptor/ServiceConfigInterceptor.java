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
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;

import java.util.Map;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;

/**
 * ServiceConfigInterceptor
 */
public class ServiceConfigInterceptor extends AbstractConfigInterceptor<ServiceConfig<?>> {

    public ServiceConfigInterceptor(Application application, PolicySupplier policySupplier) {
        super(application, policySupplier);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, String> getContext(ExecutableContext ctx) {
        return (Map<String, String>) ctx.getArguments()[2];
    }

    @Override
    protected int getRegistryType(ServiceConfig<?> config) {
        int result = 0;
        if (config.getRegistries() != null) {
            for (RegistryConfig registry : config.getRegistries()) {
                Map<String, String> map = registry.getParameters();
                if (map != null && SERVICE_REGISTRY_TYPE.equals(map.get(REGISTRY_TYPE_KEY))) {
                    result |= REGISTRY_TYPE_SERVICE;
                } else {
                    result |= REGISTRY_TYPE_INTERFACE;
                }
            }
        }
        return result;
    }

}
