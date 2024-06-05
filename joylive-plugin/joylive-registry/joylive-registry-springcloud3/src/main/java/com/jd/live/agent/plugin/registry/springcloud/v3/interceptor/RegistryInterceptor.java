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
package com.jd.live.agent.plugin.registry.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.bootstrap.AgentLifecycle;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.PolicyType;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.util.Map;

/**
 * RegistryInterceptor
 */
public class RegistryInterceptor extends AbstractRegistryInterceptor {

    private final PolicySupplier policySupplier;

    public RegistryInterceptor(Application application, AgentLifecycle lifecycle, PolicySupplier policySupplier) {
        super(application, lifecycle);
        this.policySupplier = policySupplier;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Registration registration = (Registration) ctx.getArguments()[0];
        attachTag(registration);
        subscribePolicy(registration);
        super.onEnter(ctx);
    }

    @Override
    protected String getServiceName(MethodContext ctx) {
        return ((Registration) ctx.getArgument(0)).getServiceId();
    }

    private void subscribePolicy(Registration registration) {
        policySupplier.subscribe(registration.getServiceId(), PolicyType.SERVICE_POLICY);
    }

    private void attachTag(Registration registration) {
        Map<String, String> metadata = registration.getMetadata();
        if (metadata != null) {
            application.label(metadata::putIfAbsent);
        }
    }
}
