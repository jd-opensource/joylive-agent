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
package com.jd.live.agent.governance.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.AgentLifecycle;
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;

/**
 * AbstractRegistryInterceptor
 */
public abstract class AbstractRegistryInterceptor extends InterceptorAdaptor {

    private final Logger logger = LoggerFactory.getLogger(AbstractRegistryInterceptor.class);

    protected final Application application;

    protected final AgentLifecycle lifecycle;

    protected final Registry registry;

    public AbstractRegistryInterceptor(Application application, AgentLifecycle lifecycle, Registry registry) {
        this.application = application;
        this.lifecycle = lifecycle;
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        if (application.getStatus() == AppStatus.STARTING) {
            ServiceInstance instance = getInstance(mc);
            if (instance != null) {
                logger.info("Delay registration until application is ready, service=" + instance.getService());
                lifecycle.addReadyHook(() -> {
                    logger.info("Register when application is ready, service=" + instance.getService());
                    registry.register(instance);
                    return mc.invokeOrigin();
                }, ctx.getType().getClassLoader());
                mc.setSkip(true);
            }
        }
    }

    protected abstract ServiceInstance getInstance(MethodContext ctx);

}
