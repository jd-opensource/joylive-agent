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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.PolicyType;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.util.Map;

/**
 * ServiceRegistryInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ServiceRegistryInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryInterceptor.class);

    private final Application application;

    private final PolicySupplier policySupplier;

    public ServiceRegistryInterceptor(Application application, PolicySupplier policySupplier) {
        this.application = application;
        this.policySupplier = policySupplier;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Registration registration = (Registration) ctx.getArguments()[0];

        attachTag(registration);
        subscribePolicy(registration);

        if (logger.isInfoEnabled()) {
            logger.info("Success filling metadata for registration " + registration.getServiceId() + " in " + ctx.getTarget().getClass());
        }
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
