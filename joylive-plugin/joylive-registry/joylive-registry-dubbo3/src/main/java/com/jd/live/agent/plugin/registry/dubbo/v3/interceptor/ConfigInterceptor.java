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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.PolicyType;
import org.apache.dubbo.config.AbstractInterfaceConfig;

import java.util.Map;

/**
 * ConfigInterceptor
 */
public abstract class ConfigInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ConfigInterceptor.class);

    protected final Application application;

    protected final PolicySupplier policySupplier;

    public ConfigInterceptor(Application application, PolicySupplier policySupplier) {
        this.application = application;
        this.policySupplier = policySupplier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        Map<String, String> map = (Map<String, String>) ((MethodContext) ctx).getResult();
        AbstractInterfaceConfig config = (AbstractInterfaceConfig) ctx.getTarget();

        attachTag(map);
        subscribePolicy(config);

        if (logger.isInfoEnabled()) {
            logger.info("success filling metadata for registration " + config.getInterface() + " in " + config.getClass());
        }
    }

    protected void subscribePolicy(AbstractInterfaceConfig config) {
        String service = getService(config);
        policySupplier.subscribe(service == null ? config.getInterface() : service, PolicyType.SERVICE_POLICY);
    }

    protected String getService(AbstractInterfaceConfig config) {
        return config.getInterface();
    }

    private void attachTag(Map<String, String> map) {
        application.label(map::put);
    }
}
