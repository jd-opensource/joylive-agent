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

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.policy.PolicySupplier;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ReferenceConfig;

/**
 * ReferenceConfigInterceptor
 */
public class ReferenceConfigInterceptor extends ConfigInterceptor {

    public ReferenceConfigInterceptor(Application application, PolicySupplier policySupplier) {
        super(application, policySupplier);
    }

    @Override
    protected String getService(AbstractInterfaceConfig config) {
        String providedBy = ((ReferenceConfig<?>) config).getProvidedBy();
        return providedBy != null ? providedBy : config.getInterface();
    }
}
