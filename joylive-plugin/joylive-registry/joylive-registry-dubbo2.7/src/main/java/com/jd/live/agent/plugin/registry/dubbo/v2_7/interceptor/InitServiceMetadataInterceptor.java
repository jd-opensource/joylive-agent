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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.PolicyType;
import org.apache.dubbo.config.*;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;

/**
 * InitServiceMetadataInterceptor
 */
public class InitServiceMetadataInterceptor extends InterceptorAdaptor {

    private static final int REGISTRY_TYPE_SERVICE = 2;

    private static final int REGISTRY_TYPE_INTERFACE = 1;

    private final PolicySupplier policySupplier;

    public InitServiceMetadataInterceptor(PolicySupplier policySupplier) {
        this.policySupplier = policySupplier;
    }

    /**
     * Enhanced logic before method execution
     *
     * @param ctx ExecutableContext
     * @see org.apache.dubbo.config.AbstractInterfaceConfig#initServiceMetadata(AbstractInterfaceConfig)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        AbstractInterfaceConfig config = (AbstractInterfaceConfig) ctx.getTarget();
        int type = 0;
        if (config instanceof AbstractServiceConfig) {
            type = getRegistryType((AbstractServiceConfig) config);
        } else if (config instanceof AbstractReferenceConfig) {
            type = getRegistryType((AbstractReferenceConfig) config);
        }
        if ((type & REGISTRY_TYPE_SERVICE) > 0) {
            if ((type & REGISTRY_TYPE_INTERFACE) == 0) {
                // Only register with service mode
                ApplicationConfig applicationConfig = config.getApplication();
                Map<String, String> map = applicationConfig.getParameters();
                if (map == null) {
                    map = new HashMap<>();
                    applicationConfig.setParameters(map);
                }
                map.put(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE);
            }
            policySupplier.subscribe(config.getApplication().getName(), PolicyType.SERVICE_POLICY);
        }
        if ((type & REGISTRY_TYPE_INTERFACE) > 0) {
            policySupplier.subscribe(config.getInterface(), PolicyType.SERVICE_POLICY);
        }
    }

    private int getRegistryType(AbstractServiceConfig config) {
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

    private int getRegistryType(AbstractReferenceConfig config) {
        String service = config.getProvidedBy();
        return service == null || service.isEmpty() ? REGISTRY_TYPE_INTERFACE : REGISTRY_TYPE_SERVICE;
    }

}
