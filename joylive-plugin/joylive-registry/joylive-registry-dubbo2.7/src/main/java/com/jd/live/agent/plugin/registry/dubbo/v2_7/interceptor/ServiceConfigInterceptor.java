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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.registry.RegisterMode;
import com.jd.live.agent.governance.registry.RegisterType;
import com.jd.live.agent.governance.registry.Registry;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;

import java.util.Map;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;

/**
 * ServiceConfigInterceptor
 */
public class ServiceConfigInterceptor extends AbstractConfigInterceptor<ServiceConfig<?>> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConfigInterceptor.class);

    public ServiceConfigInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected RegisterType getRegisterType(ServiceConfig<?> config) {
        int mode = 0;
        if (config.getRegistries() != null) {
            for (RegistryConfig registry : config.getRegistries()) {
                Map<String, String> map = registry.getParameters();
                if (map != null && SERVICE_REGISTRY_TYPE.equals(map.get(REGISTRY_TYPE_KEY))) {
                    mode |= RegisterMode.INSTANCE.getValue();
                } else {
                    mode |= RegisterMode.INTERFACE.getValue();
                }
            }
        }
        switch (mode) {
            case 2:
                return new RegisterType(RegisterMode.INSTANCE, config.getApplication().getName(), config.getInterface(), config.getGroup());
            case 3:
                return new RegisterType(RegisterMode.ALL, config.getApplication().getName(), config.getInterface(), config.getGroup());
            default:
                return new RegisterType(RegisterMode.INTERFACE, config.getApplication().getName(), config.getInterface(), config.getGroup());

        }
    }

    @Override
    protected void subscribe(String service, String group) {
        registry.register(service, group);
        logger.info("Found dubbo provider, service: {}, group: {}", service, group);
    }

    @Override
    protected Map<String, String> getContext(ExecutableContext ctx) {
        return ctx.getArgument(2);
    }

}
