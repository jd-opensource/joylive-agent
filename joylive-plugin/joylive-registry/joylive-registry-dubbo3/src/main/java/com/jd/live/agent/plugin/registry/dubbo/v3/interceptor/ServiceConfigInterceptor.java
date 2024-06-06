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
import com.jd.live.agent.governance.policy.PolicyType;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;

import java.util.Map;

import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INSTANCE;

/**
 * ServiceRegistrationInterceptor
 */
public class ServiceConfigInterceptor extends InterceptorAdaptor {

    private final Application application;

    private final PolicySupplier policySupplier;

    public ServiceConfigInterceptor(Application application, PolicySupplier policySupplier) {
        this.application = application;
        this.policySupplier = policySupplier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext methodContext = (MethodContext) ctx;

        Map<String, String> map = (Map<String, String>) methodContext.getResult();
        application.label(map::putIfAbsent);

        // TODO ALL mode, which includes two types.
        AbstractInterfaceConfig config = (AbstractInterfaceConfig) ctx.getTarget();
        ApplicationConfig application = config.getApplication();
        String registerMode = application.getRegisterMode();
        String service = DEFAULT_REGISTER_MODE_INSTANCE.equals(registerMode) ? application.getName() : config.getInterface();
        policySupplier.subscribe(service, PolicyType.SERVICE_POLICY);
    }
}
