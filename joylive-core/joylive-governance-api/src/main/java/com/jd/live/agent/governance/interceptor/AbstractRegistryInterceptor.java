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

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.AgentLifecycle;
import com.jd.live.agent.core.instance.Application;

/**
 * An abstract interceptor that delays the registration of services until the application is ready.
 * This class extends {@link ReadyExecutionInterceptor} and provides additional logging functionality.
 */
public abstract class AbstractRegistryInterceptor extends ReadyExecutionInterceptor {

    private final Logger logger = LoggerFactory.getLogger(AbstractRegistryInterceptor.class);

    public AbstractRegistryInterceptor(Application application, AgentLifecycle lifecycle) {
        super(application, lifecycle);
    }

    /**
     * Gets the service name from the given method context.
     *
     * @param ctx the method context
     * @return the service name
     */
    protected abstract String getServiceName(MethodContext ctx);

    @Override
    protected void beforeAddReadyHook(MethodContext ctx) {
        logger.info("Delay registration until application is ready, service=" + getServiceName(ctx));
    }

    @Override
    protected void beforeExecute(MethodContext ctx) {
        logger.info("Register when application is ready, service=" + getServiceName(ctx));
    }

}
