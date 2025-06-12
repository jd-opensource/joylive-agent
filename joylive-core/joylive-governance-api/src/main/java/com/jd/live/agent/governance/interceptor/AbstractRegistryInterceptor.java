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
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;

import java.util.List;

/**
 * AbstractRegistryInterceptor
 */
public abstract class AbstractRegistryInterceptor extends InterceptorAdaptor {

    protected final Application application;

    protected final Registry registry;

    public AbstractRegistryInterceptor(Application application, Registry registry) {
        this.application = application;
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        beforeRegister(mc);
        if (application.getStatus() == AppStatus.STARTING) {
            doRegister(mc);
        }
    }

    protected void doRegister(MethodContext mc) {
        List<ServiceInstance> instances = getInstances(mc);
        if (instances != null && !instances.isEmpty()) {
            registry.register(instances, () -> {
                mc.invokeOrigin();
                return null;
            });
            mc.setSkip(true);
        }
    }

    /**
     * A hook method that is executed before registering a method context.
     *
     * @param ctx the {@link MethodContext} that is about to be registered, containing
     *            information about the method and its execution context
     */
    protected void beforeRegister(MethodContext ctx) {

    }

    /**
     * Retrieves a service instance associated with the provided method context.
     *
     * @param ctx the {@link MethodContext} for which to retrieve the service instance
     * @return the {@link ServiceInstance} associated with the method context
     */
    protected List<ServiceInstance> getInstances(MethodContext ctx) {
        return null;
    }

}
