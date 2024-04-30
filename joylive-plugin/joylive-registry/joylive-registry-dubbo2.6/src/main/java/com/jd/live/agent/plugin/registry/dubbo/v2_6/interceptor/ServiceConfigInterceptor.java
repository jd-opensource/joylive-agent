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

import com.alibaba.dubbo.config.ServiceConfig;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.PolicyType;

import java.util.Map;

/**
 * ServiceConfigInterceptor
 */
public class ServiceConfigInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceConfigInterceptor.class);

    private final Application application;

    private final PolicySupplier policySupplier;

    public ServiceConfigInterceptor(Application application, PolicySupplier policySupplier) {
        this.application = application;
        this.policySupplier = policySupplier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        Map<String, String> map = (Map<String, String>) ctx.getArguments()[2];
        ServiceConfig<?> config = (ServiceConfig<?>) ctx.getTarget();

        attachTag(map);
        subscribePolicy(config);

        if (logger.isInfoEnabled()) {
            logger.info("success filling metadata for registration " + config.getInterface() + " in " + config.getClass());
        }
    }

    private void subscribePolicy(ServiceConfig<?> config) {
        policySupplier.subscribe(config.getInterface(), PolicyType.SERVICE_POLICY);
    }

    private void attachTag(Map<String, String> map) {
        application.label(map::put);
    }
}
