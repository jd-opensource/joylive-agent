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

import com.alibaba.dubbo.rpc.service.GenericService;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.doc.DocumentRegistry;
import com.jd.live.agent.governance.doc.ServiceAnchor;
import com.jd.live.agent.governance.registry.RegisterMode;
import com.jd.live.agent.governance.registry.RegisterType;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceId;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ServiceConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INSTANCE;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INTERFACE;

/**
 * ServiceRegistrationInterceptor
 */
public class ServiceConfigInterceptor extends AbstractConfigInterceptor<ServiceConfig<?>> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConfigInterceptor.class);

    private final DocumentRegistry docRegistry;

    public ServiceConfigInterceptor(Application application, Registry registry, DocumentRegistry docRegistry) {
        super(application, registry);
        this.docRegistry = docRegistry;
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
        Class<?> clazz = config.getInterfaceClass();
        if (clazz != GenericService.class) {
            docRegistry.register(() -> {
                List<ServiceAnchor> anchors = new ArrayList<>(16);
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    anchors.add(new ServiceAnchor(serviceId.getService(), serviceId.getGroup(), serviceId.isInterfaceMode() ? "/" : clazz.getName(), method.getName()));
                }
                return anchors;
            });
        }
    }

    @Override
    protected Map<String, String> getContext(ExecutableContext ctx) {
        return ((MethodContext) ctx).getResult();
    }
}
