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
import com.jd.live.agent.governance.registry.RegisterMode;
import com.jd.live.agent.governance.registry.RegisterType;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceId;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ServiceConfig;

import java.util.Map;

import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INSTANCE;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INTERFACE;

/**
 * ServiceRegistrationInterceptor
 */
public class ServiceConfigInterceptor extends AbstractConfigInterceptor<ServiceConfig<?>> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConfigInterceptor.class);

    public ServiceConfigInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected RegisterType getServiceId(ServiceConfig<?> config) {
        ApplicationConfig appCfg = config.getApplication();
        String registerMode = appCfg.getRegisterMode();
        if (DEFAULT_REGISTER_MODE_INTERFACE.equals(registerMode)) {
            return new RegisterType(RegisterMode.INTERFACE, appCfg.getName(), config.getInterface(), config.getGroup());
        } else if (DEFAULT_REGISTER_MODE_INSTANCE.equals(registerMode)) {
            return new RegisterType(RegisterMode.INSTANCE, appCfg.getName(), config.getInterface(), config.getGroup());
        } else {
            return new RegisterType(RegisterMode.ALL, appCfg.getName(), config.getInterface(), config.getGroup());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void subscribe(ServiceConfig<?> config, ServiceId serviceId) {
        registry.register(serviceId);
        logger.info("Found dubbo provider {}.", serviceId.getUniqueName());
    }

    @Override
    protected Map<String, String> getContext(ExecutableContext ctx) {
        return ((MethodContext) ctx).getResult();
    }
}
