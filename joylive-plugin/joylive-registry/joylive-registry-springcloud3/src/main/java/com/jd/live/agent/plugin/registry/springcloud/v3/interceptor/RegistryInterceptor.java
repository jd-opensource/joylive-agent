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

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.plugin.registry.springcloud.v3.registry.LiveRegistration;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * RegistryInterceptor
 */
public class RegistryInterceptor extends AbstractRegistryInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RegistryInterceptor.class);
    private static final String REGISTRATION = "registration";

    public RegistryInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected void beforeRegister(MethodContext ctx) {
        Object[] arguments = ctx.getArguments();
        LiveRegistration registration = new LiveRegistration((Registration) arguments[0], application);
        ctx.setAttribute(REGISTRATION, registration);
        registry.register(registration.getServiceId());
        logger.info("Found spring cloud provider, service:{}, metadata:{}", registration.getServiceId(), registration.getMetadata());
    }

    @Override
    protected List<ServiceInstance> getInstances(MethodContext ctx) {
        LiveRegistration registration = ctx.getAttribute(REGISTRATION);
        return toList(registration.toInstance());
    }
}
